package qingzhou.engine.util.pattern;

public interface Process {
    void exec() throws Exception;

    default void undo() {
    }
}
