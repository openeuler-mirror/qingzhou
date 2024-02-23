package qingzhou.app.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import qingzhou.app.AppManager;
import qingzhou.framework.Framework;
import qingzhou.framework.util.FileUtil;
import qingzhou.logger.Logger;

import java.io.File;
import java.util.Arrays;

public class Controller implements BundleActivator {
    private ServiceReference<Logger> loggerReference;
    private ServiceReference<Framework> frameworkContextReference;

    private AppManager appManager;
    public static Logger logger;
    private ServiceRegistration<AppManager> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        loggerReference = context.getServiceReference(Logger.class);
        frameworkContextReference = context.getServiceReference(Framework.class);

        Controller.logger = context.getService(loggerReference);
        Framework framework = context.getService(frameworkContextReference);
        this.appManager = new AppManagerImpl(framework);
        startAppManager(framework);
        registration = context.registerService(AppManager.class, appManager, null);
    }

    @Override
    public void stop(BundleContext context) {
        context.ungetService(frameworkContextReference);
        context.ungetService(loggerReference);
        registration.unregister();

        stopAppManager();
    }

    private void startAppManager(Framework framework) throws Exception {
        File nodeAgentApp = FileUtil.newFile(framework.getLib(), "module", "qingzhou-app", "nodeagent");
        appManager.installApp(nodeAgentApp);

        File[] files = FileUtil.newFile(framework.getDomain(), "apps").listFiles();
        if (files != null) {
            for (File file : files) {
                appManager.installApp(file);
            }
        }
    }

    private void stopAppManager() {
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
