package qingzhou.engine;

public interface Module {
    void start(ModuleContext moduleContext) throws Exception;

    void stop();
}
