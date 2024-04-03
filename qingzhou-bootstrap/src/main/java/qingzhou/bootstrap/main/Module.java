package qingzhou.bootstrap.main;

public interface Module {
    void start(FrameworkContext frameworkContext) throws Exception;

    void stop();
}
