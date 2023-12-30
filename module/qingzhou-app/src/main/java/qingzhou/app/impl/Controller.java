package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.logger.Logger;
import qingzhou.logger.LoggerService;

import java.io.File;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private AppManager appManager;
    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        FrameworkContext frameworkContext = context.getService(serviceReference);
        appManager = frameworkContext.getAppInfoManager();
        logger = frameworkContext.getService(LoggerService.class).getLogger();

        File[] files = new File(frameworkContext.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = buildAppName(file);
                logger.info("install app: " + appName);
                appManager.installApp(appName, file);
            }
        }
    }

    private String buildAppName(File file) {
        String appType = ".jar";
        String fileName = file.getName();
        if (!fileName.endsWith(appType)) {
            throw new IllegalArgumentException(fileName);
        }
        return fileName.substring(0, fileName.length() - appType.length());
    }

    @Override
    public void stop(BundleContext context) {
        appManager.getApps().forEach(appName -> {
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
