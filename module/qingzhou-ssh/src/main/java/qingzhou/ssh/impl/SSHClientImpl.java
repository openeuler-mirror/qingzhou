package qingzhou.ssh.impl;

import org.apache.sshd.client.session.ClientSession;
import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHClientConfig;
import qingzhou.ssh.SSHResult;
import qingzhou.ssh.SSHSession;

import java.io.IOException;
import java.util.Map;

public class SSHClientImpl implements SSHClient {

    private SSHSessionImpl sshSession;

    public SSHClient start(Map<String, String> config) throws IOException {
        SSHClientConfig sshClientConfig = new SSHClientConfig()
                .setHostname(config.get("ip"))
                .setPort(Integer.parseInt(config.getOrDefault("sshPort", "22")))
                .setUsername(config.get("sshUserName"))
                .setKeyPairType(config.get("keyPairType"))
                .setPrivateKeyLocation(config.get("privateKeyLocation"))
                .setPassword(config.get("sshPassword"));

        this.sshSession = new SSHSessionImpl(sshClientConfig);

        return this;
    }

    @Override
    public SSHSession getSSHSession() {
        return sshSession;
    }

    @Override
    public SSHResult execCmd(String cmd) throws Exception {
        try (ClientSession session = sshSession.createSession()) {
            return sshSession.execCmd0(session, cmd);
        }
    }

    @Override
    public SSHResult execShell(String cmd) throws Exception {
        try (ClientSession session = sshSession.createSession()) {
            return sshSession.execCmd0(session, "source /etc/profile; source ~/.bash_profile; " + cmd);
        }
    }

    @Override
    public String uploadFile(String src, String dist) throws IOException {
        try (ClientSession session = sshSession.createSession()) {
            return sshSession.fileProcess(session, src, dist, "upload");
        }
    }

    @Override
    public String downLoadFile(String src, String dist) throws IOException {
        try (ClientSession session = sshSession.createSession()) {
            return sshSession.fileProcess(session, dist, src, "download");
        }
    }

    @Override
    public void close() {
        if (sshSession != null) {
            sshSession.close();
        }
    }
}
