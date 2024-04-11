package qingzhou.app.impl;

import qingzhou.app.AppInfo;
import qingzhou.app.AppManager;
import qingzhou.engine.ServiceRegister;
import qingzhou.engine.util.FileUtil;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.Arrays;

public class Controller extends ServiceRegister<AppManager> {
    public static Logger logger;
    public static AppManager appManager;

    @Override
    public Class<AppManager> serviceType() {
        return AppManager.class;
    }

    @Override
    protected AppManager serviceObject() {
        return appManager;
    }

    @Override
    protected void startService() throws Exception {
        super.startService();

        Controller.logger = moduleContext.getService(Logger.class);
        appManager = new AppManagerImpl(moduleContext);

        File masterApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-app", AppInfo.SYS_APP_MASTER);
        appManager.installApp(masterApp);

        File nodeAgentApp = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-app", AppInfo.SYS_APP_NODE_AGENT);
        appManager.installApp(nodeAgentApp);

        File[] files = FileUtil.newFile(moduleContext.getInstanceDir(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                appManager.installApp(file);
            }
        }
    }

    @Override
    protected void stopService() {
        super.stopService();

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
