package qingzhou.ai.memory.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话管理：列表、完整消息、重命名、删除。查询视图只承载对外字段，不参与持久化
 */
final class ConversationAdmin {
    private static final String SUMMARY_MESSAGE_ID = "summary";

    private final ConversationRepository repository;
    private final MemoryLimits limits;

    ConversationAdmin(ConversationRepository repository, MemoryLimits limits) {
        this.repository = repository;
        this.limits = limits;
    }

    int titleMaxChars() {
        return limits.titleMaxChars;
    }

    /**
     * 某用户的全部会话摘要（按 updatedAt 倒序：活跃会话靠前），走内存索引
     */
    List<ConversationSummary> list(String userId) {
        List<ConversationSummary> conversations = new ArrayList<>();
        synchronized (repository) {
            for (CatalogEntry entry : repository.catalog().byUser(userId)) {
                conversations.add(new ConversationSummary(entry.id, entry.title, entry.updatedAt));
            }
        }
        conversations.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return conversations;
    }

    /**
     * 完整消息列表（时间正序）：空内容条目跳过；会话不存在或归属不符返回空列表；
     * 已归档会话返回一条摘要消息，避免旧会话在前端凭空消失
     */
    List<HistoryMessage> messages(String userId, String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        synchronized (repository) {
            ConversationRecord record = repository.ownedRecord(userId, conversationId);
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
        }
        return messages;
    }

    /**
     * 重命名（title null 表示未命名）：归属不符或不存在且无标题返回 false；
     * 不存在且带标题时创建（首条消息在回答完成后才落库，rename 先到属正常时序）
     */
    boolean rename(String userId, String conversationId, String title) {
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null) {
                if (title == null) return false; // 不凭空产生未命名会话
                record = new ConversationRecord();
                record.userId = userId;
            } else if (!userId.equals(record.userId)) {
                return false;
            }
            record.title = title;
            record.updatedAt = System.currentTimeMillis();
            if (record.createdAt == 0) record.createdAt = record.updatedAt;
            repository.persist(conversationId, record);
            return true;
        }
    }

    /**
     * 删除本人会话：命中返回 true；会话不存在或归属不符返回 false（调用方 404），数据保留
     */
    boolean remove(String userId, String conversationId) {
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null || !userId.equals(record.userId)) return false;
            repository.delete(conversationId);
            return true;
        }
    }

    /**
     * 删除本人会话内的单条消息：命中返回 true；会话不存在、归属不符、已归档或消息不存在返回 false（调用方 404）。
     * 删除不更新 updatedAt（删除不该把会话顶到列表最前）；消息删空后连会话一起删，避免零消息空壳
     */
    boolean removeMessage(String userId, String conversationId, String messageId) {
        synchronized (repository) {
            ConversationRecord record = repository.load(conversationId);
            if (record == null || record.archived || !userId.equals(record.userId) || record.messages == null) {
                return false;
            }
            boolean removed = record.messages.removeIf(message -> message.id != null && message.id.equals(messageId));
            if (!removed) return false;
            if (record.messages.isEmpty()) {
                repository.delete(conversationId);
            } else {
                repository.persist(conversationId, record);
            }
            return true;
        }
    }
}
