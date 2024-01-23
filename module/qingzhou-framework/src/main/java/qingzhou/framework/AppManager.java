package qingzhou.framework;

import java.util.Set;

public interface AppManager {
    void addApp(String name, App app) throws Exception;

    App removeApp(String name) throws Exception;

    Set<String> getApps();

    App getApp(String name);
}
