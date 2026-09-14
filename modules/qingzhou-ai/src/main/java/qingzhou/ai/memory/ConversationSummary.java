package qingzhou.ai.memory;

/** 会话列表条目（GET /conversations 响应体）：conversationId/title/updatedAt，userId 永不外泄 */
public final class ConversationSummary {
    public final String conversationId;
    public final String title;
    public final long updatedAt;

    public ConversationSummary(String conversationId, String title, long updatedAt) {
        this.conversationId = conversationId;
        this.title = title;
        this.updatedAt = updatedAt;
    }
}
