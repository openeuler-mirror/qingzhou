package qingzhou.ai.memory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.*;
import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.llm.ChatModelFactory;
import qingzhou.logger.Logger;
import qingzhou.store.Store;
import qingzhou.store.StoreFactory;

/**
 * 会话记忆：容量有界 + 长会话可续。
 * 占用有界靠三层降级而非直接丢弃：先压缩（保留摘要）→ 再归档（只留标题与摘要）→ 最后才删除。
 * KV 仍是唯一正本，{@link ConversationCatalog} 是纯派生索引，可随时从 KV 重建
 */
@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = ConversationStore.class)
public class ConversationStore {
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}"); // 防路径遍历
    private static final String SUMMARY_MESSAGE_ID = "summary";

    /**
     * 会话/消息 id 规则：各入口复用同一规则，非法 id 无法落库，须在写入前拒绝
     */
    public static boolean isValidId(String id) {
        return id != null && VALID_ID.matcher(id).matches();
    }

    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private StoreFactory storeFactory;
    // 可选：未加载 llm 时仅丧失摘要能力，会话存取与查询不受影响
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile ChatModelFactory chatModelFactory;

    private Store kvStore;
    private MemoryLimits limits;
    private ConversationCatalog catalog;
    private ConversationQuota quota;
    private ConversationCompactor compactor;

    @Activate
    public void init(Map<String, String> config) {
        limits = new MemoryLimits(config);
        boolean persistent = config != null && "file".equalsIgnoreCase(config.get("memory_store"));
        if (persistent) {
            String dir = config.get("memory_store_dir");
            if (dir == null || dir.trim().isEmpty()) dir = "data/ai-conversations";
            kvStore = storeFactory.buildFileStore(Paths.get(config.get("qingzhou.instance"), dir).toFile());
        } else {
            kvStore = storeFactory.buildMemoryStore();
        }
        catalog = new ConversationCatalog();
        quota = new ConversationQuota(this, limits);
        compactor = new ConversationCompactor(this, limits, logger, () -> chatModelFactory);

        rebuild();
        synchronized (this) {
            if (!persistent) {
                // 内存后端：超限即内存已实际占用，必须立即收敛（只归档/淘汰，不调 LLM）
                quota.enforce(null);
            } else if (catalog.overQuota(limits)) {
                // 文件后端：不在启动路径做破坏性动作，交由后续写入逐步收敛
                logger.warn("conversation store already exceeds the configured quota, it will converge on subsequent writes");
            }
        }
    }

    @Deactivate
    public void destroy() {
        compactor.shutdown();
    }

    /**
     * 记忆注入上下文的消息列表：摘要置于历史之前，历史按剩余预算由新到旧截取
     */
    public synchronized List<ChatMemory.Message> getMessageList(String userId, String conversationId, int overhead) {
        List<ChatMemory.Message> result = new ArrayList<>();
        ConversationRecord record = ownedRecord(userId, conversationId);
        if (record == null) return result;

        if (record.summary != null && !record.summary.isEmpty()) {
            result.add(ChatMemory.SummaryMessage.of(record.summary));
        }
        if (record.archived || record.messages == null) return result;

        List<StoredMessage> messages = record.messages;
        // 摘要同样占用上下文，须从预算中扣除；扣完仍不低于下限
        int budget = Math.max(limits.historyMinChars,
                limits.historyBudget(overhead) - MemoryTexts.length(record.summary));
        int maxMessages = Math.max(2, limits.historyMaxMessages); // 最近 1 轮问答是硬保底，条数护栏不能压过它
        int from = messages.size();
        int chars = 0;
        while (from > 0 && messages.size() - from < maxMessages) {
            int len = MemoryTexts.length(messages.get(from - 1).content);
            boolean mustKeep = messages.size() - from < 2; // 硬保底：至少保留最近 1 轮问答
            if (!mustKeep && chars + len > budget) break;
            chars += len;
            from--;
        }
        for (StoredMessage message : messages.subList(from, messages.size())) {
            if ("user".equals(message.role)) {
                result.add(ChatMemory.UserMessage.of(message.content));
            } else if ("assistant".equals(message.role)) {
                result.add(ChatMemory.AssistantMessage.of(message.content));
            } else {
                logger.warn("Unknown Message Type: " + message.role);
            }
        }
        return result;
    }

    /**
     * 某用户的全部会话摘要（按 updatedAt 倒序：活跃会话靠前），走内存索引
     */
    synchronized List<ConversationSummary> listConversations(String userId) {
        List<ConversationSummary> conversations = new ArrayList<>();
        for (CatalogEntry entry : catalog.byUser(userId)) {
            conversations.add(new ConversationSummary(entry.id, entry.title, entry.updatedAt));
        }
        conversations.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return conversations;
    }

    /**
     * 完整消息列表（时间正序）：空内容条目跳过；会话不存在或归属不符返回空列表；
     * 已归档会话返回一条摘要消息，避免旧会话在前端凭空消失
     */
    synchronized List<HistoryMessage> listMessages(String userId, String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        ConversationRecord record = ownedRecord(userId, conversationId);
        if (record == null) return messages;

        if (record.archived) {
            String summary = record.summary == null || record.summary.isEmpty()
                    ? "（历史内容已归档）" : "（历史摘要）" + record.summary;
            messages.add(new HistoryMessage(SUMMARY_MESSAGE_ID, "assistant", summary));
            return messages;
        }
        if (record.messages == null) return messages;
        for (StoredMessage message : record.messages) {
            if (message.content == null || message.content.isEmpty()) continue;
            messages.add(new HistoryMessage(message.id, message.role, message.content));
        }
        return messages;
    }

    public String storeUserMessage(String conversationId, String userId, String content) {
        StoredMessage message = message("user", content);
        append(conversationId, userId, message);
        return message.id;
    }

    public void storeAssistantMessage(String conversationId, String userId, String content, String messageId) {
        StoredMessage message = message("assistant", content);
        message.id = messageId;
        append(conversationId, userId, message);
    }

    /**
     * 重命名（title null 表示未命名）：归属不符或不存在且无标题返回 false；
     * 不存在且带标题时创建（首条消息在回答完成后才落库，rename 先到属正常时序）
     */
    synchronized boolean rename(String userId, String conversationId, String title) {
        if (!isValidId(conversationId)) return false;
        ConversationRecord record = load(conversationId);
        if (record == null) {
            if (title == null) return false; // 不凭空产生未命名会话
            record = new ConversationRecord();
            record.id = conversationId;
            record.userId = userId;
        } else if (record.userId == null || !record.userId.equals(userId)) {
            return false;
        }
        record.title = title;
        record.updatedAt = System.currentTimeMillis();
        if (record.createdAt == 0) record.createdAt = record.updatedAt;
        return save(conversationId, record);
    }

    /**
     * 删除本人会话：命中返回 true；会话不存在或归属不符返回 false（调用方 404），数据保留
     */
    synchronized boolean remove(String userId, String conversationId) {
        ConversationRecord record = load(conversationId);
        if (record == null || record.userId == null || !record.userId.equals(userId)) return false;
        deleteRecord(conversationId);
        return true;
    }

    /**
     * 删除本人会话内的单条消息：命中返回 true；会话不存在、归属不符、已归档或消息不存在返回 false（调用方 404）。
     * 删除不更新 updatedAt（删除不该把会话顶到列表最前）；消息删空后连会话一起删，避免零消息空壳
     */
    synchronized boolean removeMessage(String userId, String conversationId, String messageId) {
        if (!isValidId(messageId)) return false;
        ConversationRecord record = load(conversationId);
        if (record == null || record.archived || record.userId == null
                || !record.userId.equals(userId) || record.messages == null) {
            return false;
        }
        boolean removed = record.messages.removeIf(message -> message.id != null && message.id.equals(messageId));
        if (!removed) return false;
        if (record.messages.isEmpty()) {
            deleteRecord(conversationId);
        } else if (!save(conversationId, record)) {
            return false;
        }
        return true;
    }

    private void append(String conversationId, String userId, StoredMessage message) {
        if (!isValidId(conversationId)) {
            logger.warn("invalid conversation id, message skipped");
            return;
        }
        boolean needCompact;
        synchronized (this) {
            ConversationRecord record = load(conversationId);
            if (record == null) {
                record = new ConversationRecord();
                record.id = conversationId;
                record.userId = userId;
                record.createdAt = message.createdAt;
            } else if (record.userId != null && !record.userId.equals(userId)) {
                logger.warn("conversation " + conversationId + " does not belong to user, message skipped");
                return;
            }
            if (record.archived) record.archived = false; // 继续对话即重新激活，原摘要仍作背景保留
            if (record.messages == null) record.messages = new ArrayList<>();
            record.messages.add(message);
            while (record.messages.size() > limits.hardMessages) { // 兜底：压缩失败时也不让单会话无界增长
                record.messages.remove(0);
            }
            record.updatedAt = message.createdAt;
            if (!save(conversationId, record)) return; // 未落库就不按陈旧索引判定配额
            quota.enforce(userId);

            CatalogEntry entry = catalog.get(conversationId);
            needCompact = entry != null && !entry.archived && entry.messageCount > limits.softMessages;
        }
        if (needCompact) compactor.submit(conversationId);
    }

    private void rebuild() {
        synchronized (this) {
            catalog.clear();
            for (String conversationId : kvStore.keys()) {
                if (!isValidId(conversationId)) continue;
                String value = kvStore.get(conversationId);
                if (value == null) continue;
                try {
                    ConversationRecord record = json.fromJson(value, ConversationRecord.class);
                    if (record == null) continue;
                    catalog.reindex(conversationId, new CatalogEntry(conversationId, record, byteSize(value)));
                } catch (Throwable t) {
                    logger.warn("failed to parse conversation: " + t.getMessage());
                }
            }
        }
    }

    /**
     * 索引的唯一写入口：先落 KV 再更新索引，KV 写失败时索引保持与旧值一致
     */
    synchronized boolean save(String conversationId, ConversationRecord record) {
        try {
            record.id = conversationId;
            String value = json.toJson(record);
            kvStore.put(conversationId, value);
            catalog.reindex(conversationId, new CatalogEntry(conversationId, record, byteSize(value)));
            return true;
        } catch (Throwable t) {
            logger.warn("failed to persist conversation: " + t.getMessage());
            return false;
        }
    }

    synchronized boolean deleteRecord(String conversationId) {
        if (!isValidId(conversationId)) return false;
        kvStore.delete(conversationId);
        catalog.reindex(conversationId, null);
        return true;
    }

    synchronized ConversationRecord load(String conversationId) {
        if (!isValidId(conversationId)) return null;
        String value = kvStore.get(conversationId);
        if (value == null) return null;
        try {
            return json.fromJson(value, ConversationRecord.class);
        } catch (Throwable t) {
            logger.warn("failed to parse conversation: " + t.getMessage());
            return null;
        }
    }

    ConversationCatalog catalog() {
        return catalog;
    }

    private ConversationRecord ownedRecord(String userId, String conversationId) {
        ConversationRecord record = load(conversationId);
        if (record != null && record.userId != null && record.userId.equals(userId)) return record;
        return null;
    }

    private StoredMessage message(String role, String content) {
        StoredMessage message = new StoredMessage();
        message.id = UUID.randomUUID().toString();
        message.role = role;
        message.content = MemoryTexts.truncate(content, limits.messageMaxChars);
        message.createdAt = System.currentTimeMillis();
        return message;
    }

    // 按字符宽度累加 UTF-8 字节数：量体积无须为每次落库再分配一份全量字节数组
    private static long byteSize(String value) {
        if (value == null) return 0;
        long bytes = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x80) {
                bytes += 1;
            } else if (c < 0x800) {
                bytes += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                bytes += 4;
                i++; // 代理对占 4 字节，跳过低位的重复计数
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }
}
