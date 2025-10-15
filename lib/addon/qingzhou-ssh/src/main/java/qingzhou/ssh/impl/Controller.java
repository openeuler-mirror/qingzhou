package qingzhou.ssh.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.ssh.Ssh;

@Module
public class Controller implements ModuleActivator {

    @Override
    public void start(ModuleContext context) {
        context.registerService(Ssh.class, SshClientBuilderImpl::new);
    }

    @Override
    public void stop() {
        SshClientBuilderImpl.getSshClientList().forEach(sshClient -> {
            try {
                sshClient.closeInternal();
            } catch (Exception ignored) {
            }
        });
    }
}
