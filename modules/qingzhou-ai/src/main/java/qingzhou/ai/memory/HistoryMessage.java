package qingzhou.ai.memory;

/**
 * 消息查询视图：只承载对外字段，不参与持久化
 */
final class HistoryMessage {
    public final String id;
    public final String role;
    public final String content;

    HistoryMessage(String id, String role, String content) {
        this.id = id;
        this.role = role;
        this.content = content;
    }
}
