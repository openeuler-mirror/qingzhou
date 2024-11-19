package qingzhou.engine;

public interface ModuleActivator {
    void start(ModuleContext context) throws Exception;

    default void stop() {
    }
}
