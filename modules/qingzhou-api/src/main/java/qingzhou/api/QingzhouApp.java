package qingzhou.api;

public interface QingzhouApp {
    void start(AppContext appContext) throws Exception;

    default void stop() {
    }
}
