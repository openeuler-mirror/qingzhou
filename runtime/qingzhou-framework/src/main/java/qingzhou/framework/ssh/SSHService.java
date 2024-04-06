package qingzhou.framework.ssh;

public interface SSHService {
    SSHClient createSSHClient(SSHConfig loginParams) throws Exception;
}
