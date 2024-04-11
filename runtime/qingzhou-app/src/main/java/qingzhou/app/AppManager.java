package qingzhou.app;

import java.io.File;
import java.util.Set;

public interface AppManager {
    void installApp(File app) throws Exception;

    void unInstallApp(String name) throws Exception;

    Set<String> getApps();

    AppInfo getApp(String name);
}
