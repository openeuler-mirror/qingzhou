package qingzhou.ssh.impl;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.core.CoreModuleProperties;
import qingzhou.ssh.SSHClient;
import qingzhou.ssh.SSHResult;
import qingzhou.ssh.SSHSession;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SSHClientImpl implements SSHClient {
    private final SSHConfig config;
    private final SshClient sshClient;
    private final List<SSHSessionImpl> sessionList = new ArrayList<>();
    private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

    SSHClientImpl(SSHConfig config) {
        this.config = config;
        this.sshClient = ClientBuilder.builder().build();
        CoreModuleProperties.IDLE_TIMEOUT.set(this.sshClient, Duration.ZERO);
        this.sshClient.start();
    }

    @Override
    public SSHSession createSession() throws Exception {
        SSHSessionImpl sshSession = createSessionInternal();
        sessionList.add(sshSession);
        sshSession.addSessionListener(() -> sessionList.remove(sshSession));
        return sshSession;
    }

    private SSHSessionImpl createSessionInternal() throws Exception {
        return new SSHSessionImpl(createClientSession());
    }

    void addSessionListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public SSHResult execCmd(String cmd) throws Exception {
        return withAutoCloseableSession(session -> session.execCmd(cmd));
    }

    @Override
    public SSHResult execCmdAsLogin(String cmd) throws Exception {
        return withAutoCloseableSession(session -> session.execCmdAsLogin(cmd));
    }

    @Override
    public void uploadFile(String src, String dist) throws Exception {
        withAutoCloseableSession((UseSession<Void>) session -> {
            session.uploadFile(src, dist);
            return null;
        });
    }

    @Override
    public void downloadFile(String src, String dist) throws Exception {
        withAutoCloseableSession((UseSession<Void>) session -> {
            session.downloadFile(src, dist);
            return null;
        });
    }

    private <T> T withAutoCloseableSession(UseSession<T> run) throws Exception {
        try (SSHSession session = createSessionInternal()) {
            return run.use(session);
        }
    }

    @Override
    public void close() throws Exception {
        closeInternal();

        lifecycleListeners.forEach(LifecycleListener::closed);
    }

    void closeInternal() throws Exception {
        sessionList.forEach(sshSession -> {
            try {
                sshSession.closeInternal();
            } catch (Exception ignored) {
            }
        });
        sessionList.clear();

        if (sshClient != null) {
            sshClient.close();
        }
    }

    ClientSession createClientSession() throws IOException {
        long connectTimeout = 10000L;
        ConnectFuture future = sshClient.connect(config.username, config.hostname, config.port).verify(connectTimeout);
        if (!future.isConnected()) {
            throw new RuntimeException("Session connect failed after " + connectTimeout + " mill seconds.");
        }
        ClientSession session = future.getSession();
        session.addPasswordIdentity(config.password);

        long authTimeout = 10000L;
        if (!session.auth().verify(authTimeout).isSuccess()) {
            session.close();
            throw new RuntimeException("Ansible control machine authentication failed after " + authTimeout + " mill seconds.");
        }

        return session;
    }

    interface UseSession<T> {
        T use(SSHSession session) throws Exception;
    }
}
