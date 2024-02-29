package qingzhou.bootstrap.main;

public interface ModuleLoader {
    void start(FrameworkContext frameworkContext) throws Exception;

    void stop(FrameworkContext frameworkContext);
}
