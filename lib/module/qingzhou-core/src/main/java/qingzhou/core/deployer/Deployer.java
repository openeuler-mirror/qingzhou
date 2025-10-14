package qingzhou.core.deployer;

import java.io.File;
import java.util.List;
import java.util.Properties;

import qingzhou.api.AuthAdapter;
import qingzhou.core.registry.AppInfo;
import qingzhou.engine.Service;

@Service(shareable = false)
public interface Deployer {
    void addAppListener(AppListener appListener);

    void removeAppListener(AppListener appListener);

    String installApp(File appDir, Properties deploymentProperties) throws Throwable;

    void unInstallApp(String appName) throws Exception;

    void startApp(String appName) throws Throwable;

    void stopApp(String appName);

    List<String> getLocalApps();

    AppManager getApp(String appName); // 返回 Local 的 App 对象

    List<String> getAllApp();

    AppInfo getAppInfo(String appName);

    AuthAdapter getAuthAdapter();
}