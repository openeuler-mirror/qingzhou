package qingzhou.crypto.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.crypto.CryptoService;

public class Controller implements BundleActivator {
    private final CryptoService cryptoService = new CryptoServiceImpl();

    private ServiceRegistration<CryptoService> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(CryptoService.class, cryptoService, null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
    }
}
