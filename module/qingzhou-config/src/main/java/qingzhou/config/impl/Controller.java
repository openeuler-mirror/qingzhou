package qingzhou.config.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import qingzhou.config.Config;
import qingzhou.crypto.CryptoService;
import qingzhou.framework.Framework;

public class Controller implements BundleActivator {
    private ServiceReference<Framework> frameworkContextReference;
    private ServiceReference<CryptoService> cryptoServiceReference;
    private ServiceRegistration<Config> registration;

    @Override
    public void start(BundleContext context) {
        frameworkContextReference = context.getServiceReference(Framework.class);
        cryptoServiceReference = context.getServiceReference(CryptoService.class);

        registration = context.registerService(Config.class, new LocalConfig(
                context.getService(frameworkContextReference),
                context.getService(cryptoServiceReference)
        ), null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
        context.ungetService(cryptoServiceReference);
        context.ungetService(frameworkContextReference);
    }
}
