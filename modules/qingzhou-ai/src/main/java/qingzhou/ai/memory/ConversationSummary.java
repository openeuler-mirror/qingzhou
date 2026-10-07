package qingzhou.ai.memory;

/**
 * 会话查询视图：只承载对外字段，不参与持久化
 */
final class ConversationSummary {
    public final String conversationId;
    public final String title;
    public final long updatedAt;

    ConversationSummary(String conversationId, String title, long updatedAt) {
        this.conversationId = conversationId;
        this.title = title;
        this.updatedAt = updatedAt;
    }
}
