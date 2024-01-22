package qingzhou.ssh.impl;

import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHConfig;
import qingzhou.ssh.SSHService;

import java.util.ArrayList;
import java.util.List;

public class SSHServiceImpl implements SSHService {
    private final List<SSHClientImpl> sshClientList = new ArrayList<>();

    @Override
    public SSHClient createSSHClient(SSHConfig loginParams) {
        SSHClientImpl sshClient = new SSHClientImpl(loginParams);
        sshClientList.add(sshClient);
        sshClient.addSessionListener(() -> sshClientList.remove(sshClient));
        return sshClient;
    }

    public List<SSHClientImpl> getSshClientList() {
        return sshClientList;
    }
}
