package qingzhou.ssh;

import qingzhou.framework.ssh.SSHClient;
import qingzhou.framework.ssh.SSHConfig;
import qingzhou.framework.ssh.SSHService;

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
