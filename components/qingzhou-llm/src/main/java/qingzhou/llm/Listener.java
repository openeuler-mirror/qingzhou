package qingzhou.llm;

public interface Listener {
    void onBegin();

    void onReasoning(String content);

    void onReasoningPause();

    void onReasoningResume();

    void onToolCall(String toolName);

    void onMessage(String content);

    void onComplete();

    void onError(Throwable t);
}
