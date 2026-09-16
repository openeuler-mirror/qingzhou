package qingzhou.ai.memory;

public class HistoryMessage {
    public final String role;
    public final String content;

    public HistoryMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
