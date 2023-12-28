package qingzhou.api;

public interface QingZhouApp {
    void install(AppContext context);

    void start(AppContext context) throws Exception;

    void stop(AppContext context);

    void uninstall(AppContext context);
}
