package qingzhou.llm;

public interface Listener {
    default void onBegin(){}

    void onReasoning(String content);

    void onMessage(String content);

    void onError(Throwable t);

    void onComplete();
}
