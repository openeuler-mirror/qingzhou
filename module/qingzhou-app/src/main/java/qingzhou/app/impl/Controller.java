package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.util.FileUtil;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;
import java.util.Arrays;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;
    private AppManager appManager;
    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(serviceReference);
        appManager = frameworkContext.getAppManager();
        logger = frameworkContext.getService(LoggerService.class).getLogger();

        if (frameworkContext.isMaster()) {
            installMasterApp();
        } else {
            installNodeApp();
        }

        installApps();
    }

    private void installMasterApp() throws Exception {
        logger.info("install master app");
        File masterApp = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "master");
        frameworkContext.getAppManager().installApp(ConsoleConstants.MASTER_APP_NAME, masterApp);
    }

    private void installNodeApp() throws Exception {
        logger.info("install node app");
        File nodeApp = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "node");
        appManager.installApp(ConsoleConstants.NODE_APP_NAME, nodeApp);
    }

    private void installApps() throws Exception {
        File[] files = new File(frameworkContext.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = file.getName();
                logger.info("install app: " + appName);
                appManager.installApp(appName, file);
            }
        }
    }

    @Override
    public void stop(BundleContext context) {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                appManager.uninstallApp(appName);
                logger.info("uninstall app: " + appName);
            } catch (Exception e) {
                logger.warn("failed to uninstall app: " + appName, e);
            }
        });
        context.ungetService(serviceReference);
    }
}
