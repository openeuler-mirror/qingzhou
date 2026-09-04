package qingzhou.llm;

public interface Listener {
    void onReasoning(String content);

    void onReasoningPause();

    void onToolCall(String toolName);

    void onMessage(String content);

    void onComplete();

    void onError(String error);

    /**
     * 本轮请求的 token 用量统计（需服务端支持 stream_options.include_usage）。
     * 工具调用多轮时每轮回调一次，由调用方决定是否累加。
     */
    void onUsage(int promptTokens, int completionTokens, int totalTokens);

    /**
     * 阶段状态提示：模型实现可在长耗时/静默阶段主动上报阶段语义（matching / working 等），
     * 客户端据此把“等待中”替换为更具体的文案。
     */
    void onStatus(String stage);
}
