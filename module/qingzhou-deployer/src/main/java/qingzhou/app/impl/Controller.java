package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppDeployer;
import qingzhou.framework.AppManager;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Logger;
import qingzhou.framework.util.FileUtil;

import java.io.File;
import java.util.Arrays;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    private FrameworkContext frameworkContext;
    private AppManager appManager;
    private AppDeployer appDeployer;
    private FileManager fileManager;
    private Logger logger;

    @Override
    public void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(serviceReference);
        appManager = frameworkContext.getAppManager();
        fileManager = frameworkContext.getFileManager();
        logger = frameworkContext.getLogger();
        appDeployer = new AppDeployerImpl(frameworkContext);
        frameworkContext.setAppDeployer(appDeployer);

        if (frameworkContext.isMaster()) {
            installMasterApp();
        }

        installNodeApp();

        installApps();
    }

    private void installMasterApp() throws Exception {
        logger.info("install master app");
        File masterApp = FileUtil.newFile(fileManager.getLib(), "sysapp", "master");
        installApp(FrameworkContext.MASTER_APP_NAME, masterApp);
    }

    private void installNodeApp() throws Exception {
        logger.info("install node app");
        File nodeApp = FileUtil.newFile(fileManager.getLib(), "sysapp", "node");
        installApp(FrameworkContext.NODE_APP_NAME, nodeApp);
    }

    private void installApps() throws Exception {
        File[] files = new File(fileManager.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                String appName = file.getName();
                logger.info("install app: " + appName);
                installApp(appName, file);
            }
        }
    }

    private void installApp(String name, File app) throws Exception {
        appDeployer.installApp(name, app);
        if (frameworkContext.isMaster()) {
            frameworkContext.getAppStubManager().registerAppStub(name, appManager.getApp(name).getAppContext().getConsoleContext());
        }
    }

    @Override
    public void stop(BundleContext context) {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                AppImpl app = (AppImpl) appManager.removeApp(appName);
                if (app != null) {
                    app.getLoader().close();
                }
                logger.info("uninstall app: " + appName);
            } catch (Exception e) {
                logger.warn("failed to uninstall app: " + appName, e);
            }
        });
        context.ungetService(serviceReference);
    }
}
