package qingzhou.ssh.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.ssh.SSHService;

@Module
public class Controller implements ModuleActivator {

    @Override
    public void start(ModuleContext context) {
        context.registerService(SSHService.class, SSHClientBuilderImpl::new);
    }

    @Override
    public void stop() {
        SSHClientBuilderImpl.getSshClientList().forEach(sshClient -> {
            try {
                sshClient.closeInternal();
            } catch (Exception ignored) {
            }
        });
    }
}
