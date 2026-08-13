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
}
