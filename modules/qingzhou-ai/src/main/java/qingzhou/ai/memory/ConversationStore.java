package qingzhou.ai.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import qingzhou.json.Json;
import qingzhou.kv.KeyValueStore;
import qingzhou.llm.HistoryMessage;
import qingzhou.logger.Logger;

/**
 * 会话存储（记忆编排上移 qingzhou-ai 的核心组件）：会话记录落 KV namespace {@link #NS}，
 * 按 qingzhou-ai.memory.type=file|memory 显式启用；未配置/类型非法/对应后端不可用时不注册服务，
 * AiChat 等消费方退化为单轮无记忆。归属判定与标题维护不在本类（经 {@link ConversationIndex}）。
 * <p>
 * 装配模式同原 ChatMemoryProvider：本组件激活时手动注册 ConversationStore 与 ConversationIndex
 * 两个服务，配置从 qingzhou-ai（configurationPid）读取；所有方法不抛异常（失败记日志返回空值/false），
 * 记忆故障不得影响对话主流程。
 */
@Component(immediate = true, configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ConversationStore {
    /** 会话记录所在 KV namespace（文件后端目录 data/kv/ai-chat/） */
    public static final String NS = "ai-chat";
    /** 单会话保留消息条数上限：超出丢最旧（与原 FileChatMemory 语义一致） */
    static final int MAX_MESSAGES_PER_CONVERSATION = 200;
    /** 单条消息内容上限：超长正文截断进历史（与原语义一致） */
    static final int MAX_CONTENT_CHARS = 8000;

    @Reference(target = "(type=file)")
    private volatile KeyValueStore fileKv;
    @Reference(target = "(type=memory)")
    private volatile KeyValueStore memoryKv;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private KeyValueStore kv;
    private ServiceRegistration<ConversationStore> registration;
    private ServiceRegistration<ConversationIndex> indexRegistration;

    /** DS 反射实例化入口；SCR 2.x 通过 Class.getConstructors() 查找，必须是 public 0 参构造器 */
    public ConversationStore() {
    }

    /** 包级编程式装配入口（DS 默认构造之外，供单元测试直接绑定后端实现） */
    ConversationStore(KeyValueStore kv, Json json, Logger logger) {
        this.kv = kv;
        this.json = json;
        this.logger = logger;
    }

    @Activate
    void init(ComponentContext ctx, Map<String, Object> config) {
        Object type = config == null ? null : config.get("memory.type");
        if (type == null || type.toString().trim().isEmpty()) return; // 未配置 = 不启用记忆（对话为单轮）
        switch (type.toString().trim()) {
            case "file":
                kv = fileKv;
                break;
            case "memory":
                kv = memoryKv;
                break;
            default:
                logger.warn("qingzhou-ai.memory.type=" + type + " is invalid, chat memory is disabled (expect file or memory)");
                return;
        }
        if (kv == null) return; // 对应后端未部署（如 kvstore bundle 被裁），安静降级
        this.kv = kv;
        BundleContext bc = ctx.getBundleContext();
        registration = bc.registerService(ConversationStore.class, this, null);
        indexRegistration = bc.registerService(ConversationIndex.class, new ConversationIndex(kv, json, logger), null);
    }

    @Deactivate
    void stop() {
        if (registration != null) registration.unregister();
        if (indexRegistration != null) indexRegistration.unregister();
    }

    /** 会话记录是否存在（resolve 归属判定用） */
    public boolean exists(String conversationId) {
        return kv.exists(NS, conversationId);
    }

    /** 最近 max 条历史（时间正序），供注入上下文；存储故障返回空列表 */
    public List<HistoryMessage> recentHistory(String conversationId, int max) {
        List<HistoryMessage> messages = listMessages(conversationId);
        int from = Math.max(0, messages.size() - max);
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    /** 完整消息列表（时间正序）：空内容条目跳过；存储故障返回空列表 */
    public List<HistoryMessage> listMessages(String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        try {
            ConversationRecord record = load(conversationId);
            if (record != null && record.messages != null) {
                for (StoredMessage msg : record.messages) {
                    if (msg.content == null || msg.content.isEmpty()) continue;
                    messages.add(new HistoryMessage(msg.role, msg.content));
                }
            }
        } catch (Throwable t) {
            logger.warn("failed to load conversation messages: " + t.getMessage());
        }
        return messages;
    }

    public void appendUser(String conversationId, String content) {
        StoredMessage msg = new StoredMessage();
        msg.id = UUID.randomUUID().toString();
        msg.role = "user";
        msg.content = truncate(content);
        msg.createdAt = System.currentTimeMillis();
        append(conversationId, msg);
    }

    public void appendAssistant(String conversationId, String messageId, String content,
                                int promptTokens, int completionTokens, int totalTokens) {
        StoredMessage msg = new StoredMessage();
        msg.id = messageId;
        msg.role = "assistant";
        msg.content = truncate(content);
        msg.promptTokens = promptTokens;
        msg.completionTokens = completionTokens;
        msg.totalTokens = totalTokens;
        msg.createdAt = System.currentTimeMillis();
        append(conversationId, msg);
    }

    /** 删除会话记录（索引条目由调用方经 {@link ConversationIndex#remove} 处理） */
    public boolean delete(String conversationId) {
        try {
            return kv.delete(NS, conversationId);
        } catch (Throwable t) {
            logger.warn("failed to delete conversation: " + t.getMessage());
            return false;
        }
    }

    /** 落库（经 kv.update 原子读改写）：条数超限丢最旧、刷新 updatedAt；失败仅记日志 */
    private void append(String conversationId, StoredMessage msg) {
        try {
            kv.update(NS, conversationId, current -> {
                try {
                    ConversationRecord record = current == null ? null : json.fromJson(current, ConversationRecord.class);
                    if (record == null) {
                        record = new ConversationRecord();
                        record.id = conversationId;
                        record.createdAt = msg.createdAt;
                    }
                    if (record.messages == null) record.messages = new ArrayList<>();
                    record.messages.add(msg);
                    while (record.messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                        record.messages.remove(0);
                    }
                    record.updatedAt = msg.createdAt;
                    return json.toJson(record);
                } catch (Throwable t) {
                    logger.warn("failed to persist conversation message: " + t.getMessage());
                    return current; // 解析/序列化失败保持原值：update 语义下返回 null 会误删整段会话
                }
            });
        } catch (Throwable t) {
            logger.warn("failed to persist conversation message: " + t.getMessage());
        }
    }

    private ConversationRecord load(String conversationId) {
        String value = kv.get(NS, conversationId);
        if (value == null) return null;
        try {
            return json.fromJson(value, ConversationRecord.class);
        } catch (Throwable t) {
            logger.warn("failed to parse conversation: " + t.getMessage());
            return null;
        }
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() <= MAX_CONTENT_CHARS ? text : text.substring(0, MAX_CONTENT_CHARS);
    }

    /** KV 内会话记录结构（迁自原 FileChatMemory.Record，去 userId/title：归属经索引判断、标题经索引维护） */
    public static class ConversationRecord {
        public String id;
        public long createdAt;
        public long updatedAt;
        public List<StoredMessage> messages;
    }

    /** 单条消息（迁自原 FileChatMemory.Message，token 用量为跨工具调用轮的聚合快照） */
    public static class StoredMessage {
        public String id;
        public String role;
        public String content;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public long createdAt;
    }
}
