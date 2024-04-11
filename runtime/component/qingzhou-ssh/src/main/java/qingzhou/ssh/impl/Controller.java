package qingzhou.ssh.impl;

import qingzhou.engine.ServiceRegister;
import qingzhou.ssh.SSHService;

public class Controller extends ServiceRegister<SSHService> {
    private final SSHServiceImpl sshService = new SSHServiceImpl();

    @Override
    public Class<SSHService> serviceType() {
        return SSHService.class;
    }

    @Override
    protected SSHService serviceObject() {
        return sshService;
    }

    @Override
    protected void stopService() {
        sshService.getSshClientList().forEach(sshClient -> {
            try {
                sshClient.closeInternal();
            } catch (Exception ignored) {
            }
        });
    }
}
