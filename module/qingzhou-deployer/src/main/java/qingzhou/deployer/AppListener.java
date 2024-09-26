package qingzhou.deployer;

public interface AppListener {
    void onInstalled(String appName);

    void onUninstalled(String appName);
}
