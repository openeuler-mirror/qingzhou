package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppDeployer;
import qingzhou.framework.AppManager;
import qingzhou.framework.AppStub;
import qingzhou.framework.FileManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Lang;
import qingzhou.framework.api.Logger;
import qingzhou.framework.api.MenuInfo;
import qingzhou.framework.api.ModelManager;
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
            ConsoleContext consoleContext = appManager.getApp(name).getAppContext().getConsoleContext();
            frameworkContext.getAppStubManager().registerAppStub(name, new AppStub() {
                @Override
                public ModelManager getModelManager() {
                    return consoleContext.getModelManager();
                }

                @Override
                public String getI18N(Lang lang, String key, Object... args) {
                    return consoleContext.getI18N(lang, key, args);
                }

                @Override
                public MenuInfo getMenuInfo(String menuName) {
                    return consoleContext.getMenuInfo(menuName);
                }

                @Override
                public String getEntryModel() {
                    return consoleContext.getEntryModel();
                }
            });
        }
    }

    @Override
    public void stop(BundleContext context) {
        String[] apps = appManager.getApps().toArray(new String[0]);
        Arrays.stream(apps).forEach(appName -> {
            try {
                AppImpl app = (AppImpl) appManager.getApp(appName);
                if (app != null) {
                    app.getLoader().close();
                    appDeployer.unInstallApp(appName);
                }
                logger.info("uninstall app: " + appName);
            } catch (Exception e) {
                logger.warn("failed to uninstall app: " + appName, e);
            }
        });
        context.ungetService(serviceReference);
    }
}
