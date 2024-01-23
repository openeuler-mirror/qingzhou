package qingzhou.framework;

import java.io.File;
import java.util.Set;

public interface AppManager {
    void installApp(String name, File app) throws Exception;

    void unInstallApp(String name) throws Exception;

    Set<String> getApps();

    App getApp(String name);
}
