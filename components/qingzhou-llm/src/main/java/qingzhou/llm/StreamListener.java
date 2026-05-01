package qingzhou.llm;

public interface StreamListener {
    default void onStart() {
    }

    void onMessage(String deltaContent);

    default void onStop() {
    }

    default void onFailure(Throwable throwable) {
    }
}
