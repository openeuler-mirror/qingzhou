package qingzhou.ssh;

import qingzhou.engine.Service;

@Service(name = "SSH Client", description = "A simple SSH client that can be used to send executable commands to a Linux host.")
public interface Ssh {
    SshClientBuilder createSSHClientBuilder();
}
