package qingzhou.ssh.impl;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.core.CoreModuleProperties;
import qingzhou.ssh.SshClient;
import qingzhou.ssh.SshResult;
import qingzhou.ssh.SshSession;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SshClientImpl implements SshClient {
    private final SshConfig config;
    private final org.apache.sshd.client.SshClient sshClient;
    private final List<SshSessionImpl> sessionList = new ArrayList<>();
    private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

    SshClientImpl(SshConfig config) {
        this.config = config;
        this.sshClient = ClientBuilder.builder().build();
        CoreModuleProperties.IDLE_TIMEOUT.set(this.sshClient, Duration.ZERO);
        this.sshClient.start();
    }

    @Override
    public SshSession createSession() throws Exception {
        SshSessionImpl sshSession = createSessionInternal();
        sessionList.add(sshSession);
        sshSession.addSessionListener(() -> sessionList.remove(sshSession));
        return sshSession;
    }

    private SshSessionImpl createSessionInternal() throws Exception {
        return new SshSessionImpl(createClientSession());
    }

    void addSessionListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public SshResult execCmd(String cmd) throws Exception {
        return withAutoCloseableSession(session -> session.execCmd(cmd));
    }

    @Override
    public SshResult execCmdAsLogin(String cmd) throws Exception {
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
        try (SshSession session = createSessionInternal()) {
            return run.use(session);
        }
    }

    @Override
    public void close() throws Exception {
        closeInternal();

        lifecycleListeners.forEach(LifecycleListener::onSessionClosed);
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
        T use(SshSession session) throws Exception;
    }
}
