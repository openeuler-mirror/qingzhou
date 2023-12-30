package qingzhou.framework;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Set;

public interface AppManager {
    void installApp(String name, File file) throws Exception;

    void installApp(String name, URLClassLoader loader) throws Exception;

    void uninstallApp(String name) throws Exception;

    Set<String> getApps();

    AppInfo getAppInfo(String name);
}
