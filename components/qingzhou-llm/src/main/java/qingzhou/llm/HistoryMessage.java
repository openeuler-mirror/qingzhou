package qingzhou.llm;

/**
 * 多轮对话的历史消息。
 * <p>
 * 调用方按时间正序传入，实现层将其拼接进 LLM 请求：
 * system -> history... -> 当前 user 消息。历史消息只支持纯文本
 * （图片/文档附件只在当前轮生效），角色约定 "user" / "assistant"。
 */
public class HistoryMessage {
    public final String role;
    public final String content;

    public HistoryMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
