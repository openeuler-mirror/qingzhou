package qingzhou.llm;

import java.util.Map;

public interface Listener {
    void onBegin();

    void onReasoning(String content);

    void onReasoningPause();

    void onReasoningResume();

    void onToolCall(String toolName, Map<String, Object> args, Object result);

    void onMessage(String content);

    void onComplete();

    void onError(Throwable t);
}
