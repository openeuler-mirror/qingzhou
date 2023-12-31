package qingzhou.framework;

import java.io.File;
import java.util.Set;

public interface AppManager {
    void installApp(String name, boolean includeCommon, File... file) throws Exception;

    void uninstallApp(String name) throws Exception;

    Set<String> getApps();

    AppInfo getAppInfo(String name);
}
