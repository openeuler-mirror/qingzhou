package qingzhou.ssh.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.ssh.SSHService;

@Module
public class Controller implements ModuleActivator {
    private SSHServiceImpl sshService;

    @Override
    public void start(ModuleContext context) {
        sshService = new SSHServiceImpl();
        context.registerService(SSHService.class, sshService);
    }

    @Override
    public void stop() {
        sshService.getSshClientList().forEach(sshClient -> {
            try {
                sshClient.closeInternal();
            } catch (Exception ignored) {
            }
        });
    }
}
