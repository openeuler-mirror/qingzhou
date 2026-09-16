package qingzhou.ai.memory;

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
