package qingzhou.llm;

public interface Listener {
    void onBegin();

    void onReasoning(String content);

    void onReasoningPause();

    void onToolCall(String toolName);

    void onReasoningResume();

    void onMessage(String content);

    void onComplete();

    void onError(String error);

    /**
     * 本轮请求的 token 用量统计（需服务端支持 stream_options.include_usage）。
     * 默认空实现，实现方可按需覆盖。
     */
    void onUsage(int promptTokens, int completionTokens, int totalTokens);
}
