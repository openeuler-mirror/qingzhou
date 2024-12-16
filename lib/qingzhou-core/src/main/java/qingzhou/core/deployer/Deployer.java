package qingzhou.core.deployer;

import java.io.File;
import java.util.List;
import java.util.Properties;

import qingzhou.core.registry.AppInfo;
import qingzhou.engine.Service;

@Service(shareable = false)
public interface Deployer {
    void addAppListener(AppListener appListener);

    void removeAppListener(AppListener appListener);

    void installApp(File appDir) throws Throwable;

    void installApp(File appDir, Properties deploymentProperties) throws Throwable;

    void unInstallApp(String appName) throws Exception;

    void startApp(String appName) throws Throwable;

    void stopApp(String appName) throws Exception;

    List<String> getLocalApps();

    App getApp(String appName); // 返回 Local 的 App 对象

    List<String> getAllApp();

    AppInfo getAppInfo(String appName);
}