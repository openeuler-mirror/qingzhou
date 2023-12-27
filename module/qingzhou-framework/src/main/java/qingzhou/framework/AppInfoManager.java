package qingzhou.framework;

import java.io.File;
import java.util.Set;

public interface AppInfoManager {
    void addApp(File file);

    void removeApp(String name);

    Set<String> getApps();

    AppInfo getAppInfo(String name);
}
