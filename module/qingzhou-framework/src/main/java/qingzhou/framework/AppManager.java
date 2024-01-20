package qingzhou.framework;

import java.util.Set;

public interface AppManager {
    void installApp(String name, AppInfo appInfo) throws Exception;

    AppInfo uninstallApp(String name) throws Exception;

    Set<String> getApps();

    AppInfo getAppInfo(String name);
}
