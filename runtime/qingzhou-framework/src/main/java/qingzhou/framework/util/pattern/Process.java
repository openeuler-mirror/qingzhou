package qingzhou.framework.util.pattern;

public interface Process {
    void exec() throws Exception;

    default void undo() {
    }
}
