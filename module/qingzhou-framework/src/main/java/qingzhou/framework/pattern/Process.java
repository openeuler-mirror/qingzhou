package qingzhou.framework.pattern;

public interface Process {
    void exec() throws Exception;

    default void undo() {
    }
}
