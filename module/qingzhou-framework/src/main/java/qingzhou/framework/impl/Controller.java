package qingzhou.framework.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.framework.Framework;

public class Controller implements BundleActivator {
    private ServiceRegistration<Framework> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(Framework.class, new FrameworkImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }
}
