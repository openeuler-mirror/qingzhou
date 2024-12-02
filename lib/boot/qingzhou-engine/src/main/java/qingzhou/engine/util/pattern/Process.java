package qingzhou.engine.util.pattern;

public interface Process {
    void exec() throws Throwable;

    default void undo() {
    }
}
