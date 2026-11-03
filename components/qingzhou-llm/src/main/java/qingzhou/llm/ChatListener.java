package qingzhou.llm;

public interface ChatListener {
    void onMessage(String content);

    void onComplete();

    void onError(String error);

    default void onSubscribe(Subscription s) {
    }

    default void onReasoning(String content) {
    }

    default void onReasoningPause() {
    }

    default void onToolCall(String toolCallId, String toolName, String args) {
    }

    /**
     * 本轮请求的 token 用量统计（需服务端支持 stream_options.include_usage）。
     * 工具调用多轮时每轮回调一次，由调用方决定是否累加。
     */
    default void onUsage(int promptTokens, int completionTokens, int totalTokens) {
    }

    /**
     * 阶段状态提示：模型实现可在长耗时/静默阶段主动上报阶段语义，
     * 正在做技能匹配等前置工作（同步 LLM 调用，期间不会有任何内容事件）
     */
    default void onSkillMatch() {
    }

    interface Subscription {
        void cancel();
    }
}
