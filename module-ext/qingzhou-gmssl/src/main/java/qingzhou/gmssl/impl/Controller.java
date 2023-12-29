package qingzhou.gmssl.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.FrameworkContext;
import qingzhou.gmssl.GmSSLService;

public class Controller implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;

    @Override
    public void start(BundleContext context) {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        FrameworkContext frameworkContext = context.getService(serviceReference);
        frameworkContext.registerService(GmSSLService.class, new GmSSLService() {
        });
    }

    @Override
    public void stop(BundleContext context) {
        context.ungetService(serviceReference);
    }
}
