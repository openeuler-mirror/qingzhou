package qingzhou.ssh.impl;

import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHConfig;
import qingzhou.ssh.SSHService;

public class SSHServiceImpl implements SSHService {

    @Override
    public SSHClient createSSHClient(SSHConfig loginParams) {
        return new SSHClientImpl(loginParams);
    }
}
