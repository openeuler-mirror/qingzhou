package qingzhou.ssh.impl;

import qingzhou.framework.ServiceRegister;
import qingzhou.ssh.SSHService;

public class Controller extends ServiceRegister<SSHService> {
    @Override
    protected Class<SSHService> serviceType() {
        return SSHService.class;
    }

    @Override
    protected SSHService serviceObject() {
        return new SSHServiceImpl();
    }
}
