package qingzhou.framework.api;

public interface QingZhouApp {
    void start(AppContext appContext) throws Exception;

    default void stop() throws Exception {
    }
}
