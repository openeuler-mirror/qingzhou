package qingzhou.app;

import qingzhou.bootstrap.main.service.ServiceRegister;
import qingzhou.framework.app.App;
import qingzhou.framework.app.AppManager;
import qingzhou.framework.logger.Logger;
import qingzhou.framework.util.FileUtil;

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

        Controller.logger = frameworkContext.getServiceManager().getService(Logger.class);
        appManager = new AppManagerImpl(frameworkContext);

        if (frameworkContext.isMaster()) {
            File masterApp = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", App.SYS_APP_MASTER);
            appManager.installApp(masterApp);
        }

        File nodeAgentApp = FileUtil.newFile(frameworkContext.getLib(), "module", "qingzhou-app", App.SYS_APP_NODE_AGENT);
        appManager.installApp(nodeAgentApp);

        File[] files = FileUtil.newFile(frameworkContext.getDomain(), "apps").listFiles();
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
