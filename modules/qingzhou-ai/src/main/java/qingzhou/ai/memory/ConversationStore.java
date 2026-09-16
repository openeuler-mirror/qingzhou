package qingzhou.ai.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.logger.Logger;
import qingzhou.store.Store;

@Component(service = ConversationStore.class)
public class ConversationStore {
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private Store kvStore;

    public List<ChatMemory.Message> getMessageList() {
//        int HISTORY_MAX_MESSAGES = 20; // 记忆模式下注入上下文的最大历史条数（应用侧算好窗口，llm 照单拼装）
//        List<HistoryMessage> historyMessages = store.recentHistory(conversationId, HISTORY_MAX_MESSAGES);
//        return historyMessages.stream().map(historyMessage -> {
//            if (historyMessage.role.equals("user")) {
//                return ChatMemory.UserMessage.of(historyMessage.content);
//            } else if (historyMessage.role.equals("assistant")) {
//                return ChatMemory.AssistantMessage.of(historyMessage.content);
//            } else {
//                logger.error("Unknown Message Type: " + historyMessage.role);
//                return null;
//            }
//        }).collect(Collectors.toList());
        return null;
    }

    /**
     * 会话记录是否存在（resolve 归属判定用）
     */
    public boolean exists(String conversationId) {
        return kvStore.contains(conversationId);
    }

    /**
     * 最近 max 条历史（时间正序），供注入上下文；存储故障返回空列表
     */
    public List<HistoryMessage> recentHistory(String conversationId, int max) {
        List<HistoryMessage> messages = listMessages(conversationId);
        int from = Math.max(0, messages.size() - max);
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    /**
     * 完整消息列表（时间正序）：空内容条目跳过；存储故障返回空列表
     */
    public List<HistoryMessage> listMessages(String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        ConversationRecord record = load(conversationId);
        if (record != null && record.messages != null) {
            for (StoredMessage msg : record.messages) {
                if (msg.content == null || msg.content.isEmpty()) continue;
                messages.add(new HistoryMessage(msg.role, msg.content));
            }
        }
        return messages;
    }

    public void storeUserMessage(String conversationId, String content) {
        StoredMessage msg = new StoredMessage();
        msg.id = UUID.randomUUID().toString();
        msg.role = "user";
        msg.content = truncate(content);
        msg.createdAt = System.currentTimeMillis();
        append(conversationId, msg);
    }

    public void storeAssistantMessage(String conversationId, String content) {
        StoredMessage msg = new StoredMessage();
        msg.id = UUID.randomUUID().toString();
        msg.role = "assistant";
        msg.content = truncate(content);
        msg.createdAt = System.currentTimeMillis();
        append(conversationId, msg);
    }

    public void delete(String conversationId) {
        kvStore.delete(conversationId);
    }

    private void append(String conversationId, StoredMessage msg) {
        try {
//            kvStore.update(conversationId, current -> {
//                try {
//                    ConversationRecord record = current == null ? null : json.fromJson(current, ConversationRecord.class);
//                    if (record == null) {
//                        record = new ConversationRecord();
//                        record.id = conversationId;
//                        record.createdAt = msg.createdAt;
//                    }
//                    if (record.messages == null) record.messages = new ArrayList<>();
//                    record.messages.add(msg);
//                    int MAX_MESSAGES_PER_CONVERSATION = 200; // 单会话保留消息条数上限：超出丢最旧（与原 FileChatMemory 语义一致）
//                    while (record.messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
//                        record.messages.remove(0);
//                    }
//                    record.updatedAt = msg.createdAt;
//                    return json.toJson(record);
//                } catch (Throwable t) {
//                    logger.warn("failed to persist conversation message: " + t.getMessage());
//                    return current; // 解析/序列化失败保持原值：update 语义下返回 null 会误删整段会话
//                }
//            });
        } catch (Throwable t) {
            logger.warn("failed to persist conversation message: " + t.getMessage());
        }
    }

    private ConversationRecord load(String conversationId) {
        String value = kvStore.get(conversationId);
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
        int MAX_CONTENT_CHARS = 8000; // 单条消息内容上限：超长正文截断进历史（与原语义一致）
        return text.length() <= MAX_CONTENT_CHARS ? text : text.substring(0, MAX_CONTENT_CHARS);
    }

    /**
     * KV 内会话记录结构（迁自原 FileChatMemory.Record，去 userId/title：归属经索引判断、标题经索引维护）
     */
    public static class ConversationRecord {
        public String id;
        public long createdAt;
        public long updatedAt;
        public List<StoredMessage> messages;
    }

    /**
     * 单条消息（迁自原 FileChatMemory.Message，token 用量为跨工具调用轮的聚合快照）
     */
    public static class StoredMessage {
        public String id;
        public String role;
        public String content;
        public long createdAt;
    }
}
