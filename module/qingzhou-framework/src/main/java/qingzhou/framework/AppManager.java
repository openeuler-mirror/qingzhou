package qingzhou.framework;

import java.util.Set;

public interface AppManager {
    void installApp(String name, App app) throws Exception;

    App uninstallApp(String name) throws Exception;

    Set<String> getApps();

    App getApp(String name);
}
