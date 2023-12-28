package qingzhou.framework;

import qingzhou.framework.impl.app.AppInfoImpl;

import java.io.File;
import java.util.Set;

public interface AppInfoManager {
    void addMasterApp(AppInfoImpl appInfo);

    boolean addApp(File file);

    boolean removeApp(String name);

    Set<String> getApps();

    AppInfo getAppInfo(String name);
}
