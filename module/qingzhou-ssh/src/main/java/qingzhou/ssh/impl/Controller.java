package qingzhou.ssh.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.ssh.SSHService;

public class Controller implements BundleActivator {
    private ServiceRegistration<SSHService> registration;

    @Override
    public void start(BundleContext bundleContext) {
        registration = bundleContext.registerService(SSHService.class, new SSHImpl(), null);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        registration.unregister();
    }
}
