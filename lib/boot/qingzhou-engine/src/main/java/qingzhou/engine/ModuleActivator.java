package qingzhou.engine;

public interface ModuleActivator {
    void start(ModuleContext context) throws Throwable;

    default void stop() {
    }
}
