package qingzhou.ai.memory;

/** 会话摘要（管理面列表项），不含消息内容 */
public class ConversationSummary {
    public final String conversationId;
    /** null 表示未命名（由前端决定展示文案） */
    public final String title;
    public final long updatedAt;

    public ConversationSummary(String conversationId, String title, long updatedAt) {
        this.conversationId = conversationId;
        this.title = title;
        this.updatedAt = updatedAt;
    }
}
