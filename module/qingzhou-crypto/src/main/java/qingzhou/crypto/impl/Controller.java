package qingzhou.crypto.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.crypto.CryptoService;

public class Controller implements BundleActivator {
    private ServiceRegistration<CryptoService> registration;

    @Override
    public void start(BundleContext bundleContext) {
        registration = bundleContext.registerService(CryptoService.class, new CryptoServiceImpl(), null);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        registration.unregister();
    }
}
