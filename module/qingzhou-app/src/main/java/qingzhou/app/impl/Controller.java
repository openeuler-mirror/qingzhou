package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Constants;
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

        installMaster();
        installApps();
    }

    private void installMaster() throws Exception {
        logger.info("install master app");
        File[] files = FileUtil.newFile(frameworkContext.getLib(), "sysapp", "master").listFiles();
        appManager.installApp(Constants.MASTER_APP_NAME, false, files);
    }

    private void installApps() throws Exception {
        File[] files = new File(frameworkContext.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = buildAppName(file);// todo 改为可配置？
                logger.info("install app: " + appName);
                boolean includeCommon = true;// todo 改为可配置？
                appManager.installApp(appName, includeCommon, file);
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
