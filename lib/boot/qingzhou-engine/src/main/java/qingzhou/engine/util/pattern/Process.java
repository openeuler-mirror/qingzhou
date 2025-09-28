package qingzhou.engine.util.pattern;

public interface Process {
    void run() throws Throwable;

    default void completed() {
    }
}
