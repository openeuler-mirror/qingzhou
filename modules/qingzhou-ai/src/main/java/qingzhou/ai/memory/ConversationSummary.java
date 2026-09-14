package qingzhou.ai.memory;

/** 会话列表条目（GET /conversations 响应体）：id/title/updatedAt，userId 永不外泄 */
public final class ConversationSummary {
    public final String id;
    public final String title;
    public final long updatedAt;

    public ConversationSummary(String id, String title, long updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
    }
}
