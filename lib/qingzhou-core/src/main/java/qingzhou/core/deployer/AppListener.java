package qingzhou.core.deployer;

public interface AppListener {
    default void onInstalled(String appName) {
    }

    default void onUninstalled(String appName) {
    }
}
