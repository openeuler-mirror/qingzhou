package qingzhou.core.deployer;

public interface AppListener {
    default void onAppStarted(String appName) {
    }

    default void onAppStopped(String appName) {
    }
}
