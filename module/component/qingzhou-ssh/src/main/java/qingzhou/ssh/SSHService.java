package qingzhou.ssh;

import qingzhou.engine.ServiceInfo;

public interface SSHService extends ServiceInfo {
    @Override
    default String getDescription() {
        return "Provide practical tools related to SSH client.";
    }

    SSHClient createSSHClient(SSHConfig loginParams) throws Exception;
}
