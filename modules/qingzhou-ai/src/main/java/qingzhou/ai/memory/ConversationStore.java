package qingzhou.ai.memory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.logger.Logger;
import qingzhou.store.Store;
import qingzhou.store.StoreFactory;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.OPTIONAL,
        service = ConversationStore.class)
public class ConversationStore {
    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private StoreFactory storeFactory;

    private final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}"); // 防路径遍历
    private Store kvStore;

    @Activate
    public void init(Map<String, String> config) {
        if (config != null && "file".equalsIgnoreCase(config.get("memory_store"))) {
            String dir = config.get("memory_store_dir");
            if (dir == null || dir.isEmpty()) dir = "data/ai-conversations";
            kvStore = storeFactory.buildFileStore(Paths.get(config.get("qingzhou.instance"), dir).toFile());
        } else {
            kvStore = storeFactory.buildMemoryStore();
        }
    }

    /**
     * 记忆注入上下文的消息列表
     */
    public List<ChatMemory.Message> getMessageList(String userId, String conversationId) {
        List<HistoryMessage> messages = listMessages(userId, conversationId);
        int from = messages.size();
        int chars = 0;
        int HISTORY_MAX_MESSAGES = 20; // 记忆注入上下文的最大历史条数
        int HISTORY_MAX_CHARS = 8000; // 记忆注入上下文的总字符预算
        while (from > 0 && from > messages.size() - HISTORY_MAX_MESSAGES) {
            String content = messages.get(from - 1).content;
            if (chars + content.length() > HISTORY_MAX_CHARS) break;
            chars += content.length();
            from--;
        }
        List<ChatMemory.Message> result = new ArrayList<>();
        for (HistoryMessage historyMessage : messages.subList(from, messages.size())) {
            if (historyMessage.role.equals("user")) {
                result.add(ChatMemory.UserMessage.of(historyMessage.content));
            } else if (historyMessage.role.equals("assistant")) {
                result.add(ChatMemory.AssistantMessage.of(historyMessage.content));
            } else {
                logger.warn("Unknown Message Type: " + historyMessage.role);
            }
        }
        return result;
    }

    /**
     * 某用户的全部会话摘要（按 updatedAt 倒序：活跃会话靠前）
     */
    public List<ConversationSummary> listConversations(String userId) {
        List<ConversationSummary> conversations = new ArrayList<>();
        for (String conversationId : kvStore.keys()) {
            ConversationRecord record = load(conversationId);
            if (record != null && record.userId != null && record.userId.equals(userId)) {
                conversations.add(new ConversationSummary(conversationId, record.title, record.updatedAt));
            }
        }
        conversations.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return conversations;
    }

    /**
     * 完整消息列表（时间正序）：空内容条目跳过；会话不存在或归属不符返回空列表
     */
    public List<HistoryMessage> listMessages(String userId, String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        ConversationRecord record = load(conversationId);
        if (record != null && record.userId != null && record.userId.equals(userId) && record.messages != null) {
            for (StoredMessage msg : record.messages) {
                if (msg.content == null || msg.content.isEmpty()) continue;
                messages.add(new HistoryMessage(msg.id, msg.role, msg.content));
            }
        }
        return messages;
    }

    public String storeUserMessage(String conversationId, String userId, String content) {
        StoredMessage msg = message("user", content);
        append(conversationId, userId, msg);
        return msg.id;
    }

    public void storeAssistantMessage(String conversationId, String userId, String content, String messageId) {
        StoredMessage msg = message("assistant", content);
        msg.id = messageId;
        append(conversationId, userId, msg);
    }

    /**
     * 重命名（title null 表示未命名）：归属不符或不存在且无标题返回 false；
     * 不存在且带标题时创建（首条消息在回答完成后才落库，rename 先到属正常时序）
     */
    public synchronized boolean rename(String userId, String conversationId, String title) {
        ConversationRecord record = load(conversationId);
        if (record == null) {
            if (title == null) return false; // 不凭空产生未命名会话
            record = new ConversationRecord();
            record.id = conversationId;
            record.userId = userId;
        } else if (record.userId == null || !record.userId.equals(userId)) {
            return false;
        }
        try {
            record.title = title;
            record.updatedAt = System.currentTimeMillis();
            if (record.createdAt == 0) record.createdAt = record.updatedAt;
            kvStore.put(conversationId, json.toJson(record));
            return true;
        } catch (Throwable t) {
            logger.warn("failed to rename conversation: " + t.getMessage());
            return false;
        }
    }

    /**
     * 删除本人会话：命中返回 true；会话不存在或归属不符返回 false（调用方 404），数据保留
     */
    public synchronized boolean remove(String userId, String conversationId) {
        ConversationRecord record = load(conversationId);
        if (record == null || record.userId == null || !record.userId.equals(userId)) return false;
        kvStore.delete(conversationId);
        return true;
    }

    /**
     * 删除本人会话内的单条消息：命中返回 true；会话不存在、归属不符或消息不存在返回 false（调用方 404）。
     * 删除不更新 updatedAt（删除不该把会话顶到列表最前）；消息删空后连会话一起删，避免零消息空壳
     */
    public synchronized boolean removeMessage(String userId, String conversationId, String messageId) {
        if (messageId == null || !VALID_ID.matcher(messageId).matches()) return false;
        ConversationRecord record = load(conversationId);
        if (record == null || record.userId == null || !record.userId.equals(userId) || record.messages == null) {
            return false;
        }
        boolean removed = record.messages.removeIf(msg -> msg.id != null && msg.id.equals(messageId));
        if (!removed) return false;
        try {
            if (record.messages.isEmpty()) {
                kvStore.delete(conversationId);
            } else {
                kvStore.put(conversationId, json.toJson(record));
            }
            return true;
        } catch (Throwable t) {
            logger.warn("failed to remove message: " + t.getMessage());
            return false;
        }
    }

    private StoredMessage message(String role, String content) {
        StoredMessage msg = new StoredMessage();
        msg.id = UUID.randomUUID().toString();
        msg.role = role;
        msg.content = truncate(content);
        msg.createdAt = System.currentTimeMillis();
        return msg;
    }

    /**
     * 读改写整条记录，synchronized 防并发覆盖
     */
    private synchronized void append(String conversationId, String userId, StoredMessage msg) {
        if (!VALID_ID.matcher(conversationId).matches()) {
            logger.warn("invalid conversation id, message skipped");
            return;
        }
        try {
            ConversationRecord record = load(conversationId);
            if (record == null) {
                record = new ConversationRecord();
                record.id = conversationId;
                record.userId = userId;
                record.createdAt = msg.createdAt;
            } else if (record.userId != null && !record.userId.equals(userId)) {
                logger.warn("conversation " + conversationId + " does not belong to user, message skipped");
                return;
            }
            if (record.messages == null) record.messages = new ArrayList<>();
            record.messages.add(msg);
            int MAX_MESSAGES_PER_CONVERSATION = 200; // 单会话消息条数上限
            while (record.messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                record.messages.remove(0);
            }
            record.updatedAt = msg.createdAt;
            kvStore.put(conversationId, json.toJson(record));
        } catch (Throwable t) {
            logger.warn("failed to persist conversation message: " + t.getMessage());
        }
    }

    private ConversationRecord load(String conversationId) {
        if (!VALID_ID.matcher(conversationId).matches()) return null;
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
        int MAX_CONTENT_CHARS = 8000; // 单条消息内容上限
        return text.length() <= MAX_CONTENT_CHARS ? text : text.substring(0, MAX_CONTENT_CHARS);
    }

    /**
     * KV 内会话记录结构，title 为 null 表示未命名
     */
    private static class ConversationRecord {
        public String id;
        public String userId;
        public String title;
        public long createdAt;
        public long updatedAt;
        public List<StoredMessage> messages;
    }

    private static class StoredMessage {
        public String id;
        public String role;
        public String content;
        public long createdAt;
    }

    private static class HistoryMessage {
        public final String id;
        public final String role;
        public final String content;

        public HistoryMessage(String id, String role, String content) {
            this.id = id;
            this.role = role;
            this.content = content;
        }
    }

    private static class ConversationSummary {
        public final String conversationId;
        public final String title;
        public final long updatedAt;

        public ConversationSummary(String conversationId, String title, long updatedAt) {
            this.conversationId = conversationId;
            this.title = title;
            this.updatedAt = updatedAt;
        }
    }
}
