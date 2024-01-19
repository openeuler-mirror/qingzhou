package qingzhou.framework.console;

import qingzhou.framework.AppManager;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.impl.FrameworkContextImpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConsoleContextCache {
    public static final Map<String, ConsoleContext> appConsoleContextCache = new ConcurrentHashMap<>();

    public static ConsoleContext getAppConsoleContext(String appName) {
        if (appConsoleContextCache.isEmpty()) {
            initAppsCache();
        }

        return ConsoleContextCache.appConsoleContextCache.get(appName);
    }

    public static void addAppConsoleContext(String appName, ConsoleContext context) {
        ConsoleContextCache.appConsoleContextCache.put(appName, context);
    }

    public static void removeAppConsoleContext(String appName) {
        ConsoleContextCache.appConsoleContextCache.remove(appName);
    }

    private static void initAppsCache() {
        AppManager appManager = FrameworkContextImpl.getFrameworkContext().getAppManager();
        if (appManager != null) {
            Set<String> apps = appManager.getApps();
            for (String app : apps) {
                appConsoleContextCache.put(app, appManager.getAppInfo(app).getAppContext().getConsoleContext());
            }
        }
    }

    public static ConsoleContext getMasterConsoleContext() {
        return getAppConsoleContext(ConsoleConstants.MASTER_APP_NAME);
    }

    private ConsoleContextCache() {
        initAppsCache();
    }
}
