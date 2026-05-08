package qingzhou.llm;

public interface Listener {
    void onReasoning(String content);

    void onMessage(String content);

    void onComplete();

    void onError(Throwable t);
}
