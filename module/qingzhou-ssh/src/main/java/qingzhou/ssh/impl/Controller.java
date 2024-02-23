package qingzhou.ssh.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import qingzhou.ssh.SSHService;

public class Controller implements BundleActivator {
    private final SSHServiceImpl sshService = new SSHServiceImpl();

    private ServiceRegistration<SSHService> registration;

    @Override
    public void start(BundleContext context) {
        registration = context.registerService(SSHService.class, sshService, null);
    }

    @Override
    public void stop(BundleContext context) {
        registration.unregister();
        stopService();
    }

    private void stopService() {
        sshService.getSshClientList().forEach(sshClient -> {
            try {
                sshClient.closeInternal();
            } catch (Exception ignored) {
            }
        });
    }
}
