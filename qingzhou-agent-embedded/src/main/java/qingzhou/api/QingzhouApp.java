package qingzhou.api;

import java.io.File;

/**
 * 轻舟框架核心的应用程序接口。
 * 每个具体的轻舟应用都需要实现本接口，作为应用的主入口。
 */
public interface QingzhouApp {
    AppContext getAppContext();

    void setAppContext(AppContext appContext);

    boolean available();

    void start() throws Exception;

    void stop();

    default void onAppRegistered() {
    }

    default void onAppUnregistered() {
    }

    default String[] args() {
        return new String[0];
    }
}