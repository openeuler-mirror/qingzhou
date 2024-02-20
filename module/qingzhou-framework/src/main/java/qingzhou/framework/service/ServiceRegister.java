package qingzhou.framework.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import qingzhou.framework.FrameworkContext;

public abstract class ServiceRegister<T> implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;
    protected FrameworkContext frameworkContext;
    private RegistryKey registryKey;

    @Override
    public final void start(BundleContext context) throws Exception {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        frameworkContext = context.getService(serviceReference);
        startService(frameworkContext);
        registryKey = frameworkContext.getServiceManager().registerService(serviceType(), serviceObject());
    }

    @Override
    public final void stop(BundleContext context) {
        frameworkContext.getServiceManager().unregisterService(registryKey);
        context.ungetService(serviceReference);

        stopService();
    }

    protected abstract Class<T> serviceType();

    protected abstract T serviceObject();

    protected void startService(FrameworkContext frameworkContext) throws Exception {
    }

    protected void stopService() {
    }
}
