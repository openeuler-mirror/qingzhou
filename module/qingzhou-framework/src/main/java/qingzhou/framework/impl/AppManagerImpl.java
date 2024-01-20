package qingzhou.framework.impl;

import qingzhou.framework.AppInfo;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.QingZhouApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AppManagerImpl implements AppManager {
    private final Map<String, AppInfo> appInfoMap = new HashMap<>();

    @Override
    public void installApp(String appName, AppInfo appInfo) throws Exception {
        if (appInfoMap.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        appInfoMap.put(appName, appInfo);

        QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.start(appInfo.getAppContext());
        }
    }

    @Override
    public AppInfo uninstallApp(String name) throws Exception {
        AppInfo appInfo = appInfoMap.remove(name);
        if (appInfo != null) {
            QingZhouApp qingZhouApp = appInfo.getQingZhouApp();
            if (qingZhouApp != null) {
                qingZhouApp.stop();
            }
        }

        return appInfo;
    }

    @Override
    public Set<String> getApps() {
        return appInfoMap.keySet();
    }

    @Override
    public AppInfo getAppInfo(String name) {
        return appInfoMap.get(name);
    }
}
