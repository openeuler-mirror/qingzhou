package qingzhou.logger.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.logger.LoggerService;

public class Controller implements BundleActivator {
    private ServiceRegistration<LoggerService> registerService;

    @Override
    public void start(BundleContext context) {
        registerService = context.registerService(LoggerService.class, new LoggerServiceImpl(), null);
    }

    @Override
    public void stop(BundleContext context) {
        registerService.unregister();
    }
}
