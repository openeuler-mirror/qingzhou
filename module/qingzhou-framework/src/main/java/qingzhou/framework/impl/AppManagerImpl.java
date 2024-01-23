package qingzhou.framework.impl;

import qingzhou.framework.App;
import qingzhou.framework.AppManager;
import qingzhou.framework.api.QingZhouApp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AppManagerImpl implements AppManager {
    private final Map<String, App> apps = new HashMap<>();

    @Override
    public void addApp(String appName, App app) throws Exception {
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }

        apps.put(appName, app);

        QingZhouApp qingZhouApp = app.getQingZhouApp();
        if (qingZhouApp != null) {
            qingZhouApp.start(app.getAppContext());
        }
    }

    @Override
    public App removeApp(String name) throws Exception {
        App app = apps.remove(name);
        if (app != null) {
            QingZhouApp qingZhouApp = app.getQingZhouApp();
            if (qingZhouApp != null) {
                qingZhouApp.stop();
            }
        }

        return app;
    }

    @Override
    public Set<String> getApps() {
        return apps.keySet();
    }

    @Override
    public App getApp(String name) {
        return apps.get(name);
    }
}
