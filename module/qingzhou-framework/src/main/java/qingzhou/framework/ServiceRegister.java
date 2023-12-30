package qingzhou.framework;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class ServiceRegister<T> implements BundleActivator {
    private ServiceReference<FrameworkContext> serviceReference;

    @Override
    public final void start(BundleContext context) {
        serviceReference = context.getServiceReference(FrameworkContext.class);
        FrameworkContext frameworkContext = context.getService(serviceReference);
        frameworkContext.registerService(serviceType(), serviceObject());
    }

    @Override
    public final void stop(BundleContext context) {
        context.ungetService(serviceReference);
    }

    protected abstract Class<T> serviceType();

    protected abstract T serviceObject();
}
