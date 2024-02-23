package qingzhou.logger.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.logger.Logger;

public class Controller implements BundleActivator {
    private ServiceRegistration<Logger> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(Logger.class, new LoggerImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }
}
