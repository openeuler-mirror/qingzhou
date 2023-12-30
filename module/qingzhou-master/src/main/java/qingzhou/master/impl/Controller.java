package qingzhou.master.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.AppManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.Constants;

import java.net.URLClassLoader;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    public static AppManager appManager;

    @Override
    public void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        FrameworkContext frameworkContext = context.getService(serviceReference);
        appManager = frameworkContext.getAppInfoManager();

        appManager.installApp(Constants.MASTER_APP_NAME, (URLClassLoader) Controller.class.getClassLoader());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        appManager.uninstallApp(Constants.MASTER_APP_NAME);
        context.ungetService(serviceReference);
    }
}
