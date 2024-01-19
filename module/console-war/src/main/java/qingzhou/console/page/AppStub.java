package qingzhou.console.page;

import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.console.ConsoleConstants;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AppStub {
    public static final Map<String, ConsoleContext> appStubMap = new ConcurrentHashMap<>();

    public static ConsoleContext getAppConsoleContext(String appName) {
        if (appStubMap.isEmpty()) {
            initAppsCache();
        }

        return AppStub.appStubMap.computeIfAbsent(appName, s -> {
            return null;//todo 需要获取应用的i18n信息，内存没有的需要从缓存拉取
        });
    }

    public static void addAppConsoleContext(String appName, ConsoleContext context) {
        AppStub.appStubMap.put(appName, context);
    }

    public static void removeAppConsoleContext(String appName) {
        AppStub.appStubMap.remove(appName);
    }

    private static void initAppsCache() {
        AppManager appManager = ConsoleWarHelper.getAppInfoManager();
        if (appManager != null) {
            Set<String> apps = appManager.getApps();
            for (String app : apps) {
                appStubMap.put(app, appManager.getAppInfo(app).getAppContext().getConsoleContext());
            }
        }
    }

    public static ConsoleContext getMasterConsoleContext() {
        return getAppConsoleContext(ConsoleConstants.MASTER_APP_NAME);
    }

    private AppStub() {
        initAppsCache();
    }
}
