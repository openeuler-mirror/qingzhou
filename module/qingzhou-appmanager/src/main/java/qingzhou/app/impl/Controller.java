package qingzhou.app.impl;

import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.ServiceRegister;
import qingzhou.framework.api.Logger;

import java.io.File;
import java.util.Arrays;

public class Controller extends ServiceRegister<AppManager> {
    private AppManager appManager;
    private Logger logger;

    @Override
    protected Class<AppManager> serviceType() {
        return AppManager.class;
    }

    @Override
    protected AppManager serviceObject() {
        return appManager;
    }

    @Override
    protected void startService(FrameworkContext frameworkContext) throws Exception {
        this.appManager = new AppManagerImpl(frameworkContext);
        this.logger = frameworkContext.getServiceManager().getService(Logger.class);

        appManager.installApp(frameworkContext.getConfigManager().nodeAgentApp());

        File[] files = frameworkContext.getConfigManager().appsDir().listFiles();
        if (files != null) {
            for (File file : files) {
                appManager.installApp(file);
            }
        }
    }

    @Override
    protected void stopService() {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                appManager.unInstallApp(appName);
            } catch (Exception e) {
                logger.warn("failed to stop app: " + appName, e);
            }
        });
    }
}
