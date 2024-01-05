package qingzhou.ssh.impl;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpPath;
import qingzhou.ssh.SSHClientConfig;
import qingzhou.ssh.SSHResult;
import qingzhou.ssh.SSHSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SSHSessionImpl implements SSHSession {
    private SshClient sshClient;
    private SSHClientConfig config;
    private ClientSession session;

    /**
     * session open超时时间
     */
    public final Long connectTimeout = 10000L;

    /**
     * 认证超时时间
     */
    public final Long authTimeout = 10000L;

    /**
     * 命令执行超时时间
     */
    private final Long executeTimeout = 600000L;

    public SSHSessionImpl(SSHClientConfig config) throws IOException {
        this.sshClient = ClientBuilder.builder().build();
        CoreModuleProperties.IDLE_TIMEOUT.set(sshClient, Duration.ZERO);
        sshClient.start();

        this.config = config;

        this.session = createSession();
    }

    @Override
    public SSHResult execCmd(String cmd) throws Exception {
        return execCmd0(getSession(), cmd);
    }

    @Override
    public String uploadFile(String src, String dist) throws IOException {
        return fileProcess(getSession(), src, dist, "upload");
    }

    @Override
    public String downLoadFile(String src, String dist) throws IOException {
        return fileProcess(getSession(), dist, src, "download");
    }

    public ClientSession getSession() throws IOException {
        if (session == null || session.isClosed()) {
            session = createSession();
        }

        return session;
    }

    public ClientSession createSession() throws IOException {
        ConnectFuture future = sshClient.connect(config.getUsername(), config.getHostname(), config.getPort()).verify(connectTimeout);
        if (!future.isConnected()) {
            throw new RuntimeException("Session connect failed after " + connectTimeout + " mill seconds.");
        }
        ClientSession session = future.getSession();
        if (config.getPrivateKeyLocation() != null) {
            // 基于秘钥登陆
            try {
                session.addPublicKeyIdentity(new FileKeyPairProvider(Paths.get(config.getPrivateKeyLocation()))
                        .loadKey(session, config.getKeyPairType()));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else if (config.getPassword() != null) {
            session.addPasswordIdentity(config.getPassword());
        }

        if (!session.auth().verify(authTimeout).isSuccess()) {
            session.close();
            throw new RuntimeException("Ansible control machine authentication failed after " + authTimeout + " mill seconds.");
        }

        return session;
    }

    protected SSHResult execCmd0(ClientSession session, String cmd) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ByteArrayOutputStream error = new ByteArrayOutputStream(); ChannelExec channel = session.createExecChannel(cmd)) {
            channel.setupSensibleDefaultPty();
            channel.setErr(error);
            channel.setOut(out);
            channel.open().verify(authTimeout);
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), executeTimeout);
            int exit = channel.getExitStatus();
            String msg;
            if (exit == 0) {
                msg = out.toString();
                if (msg == null || msg.isEmpty()) {
                    msg = error.toString();
                }
            } else {
                msg = error.toString();
                if (msg == null || msg.isEmpty()) {
                    msg = out.toString();
                    if (msg != null) {
                        exit = 0;
                    }
                } else {
                    System.err.println(String.format("Failed to execute command: %s, exit status: %d, details: %s", cmd, exit, msg));
                }
            }
            SSHResultImp sshResult = new SSHResultImp();
            sshResult.setCode(exit);
            sshResult.setMessage(msg);

            return sshResult;
        }
    }

    protected String fileProcess(ClientSession session, String localFile, String remoteFile, String type) {
        try (SftpFileSystem fs = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remote = fs.getDefaultDir().resolve(remoteFile);
            Path local = Paths.get(localFile);
            if ("upload".equals(type)) {
                createDirectories(remote);
                upload(local, remote, fs);
            } else if ("download".equals(type)) {
                createDirectories(local);
                download(remote, local, fs);
            }
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void upload(Path source, Path target, SftpFileSystem fs) throws IOException {
        if (Files.isDirectory(source)) {
            try (Stream<Path> stream = Files.list(source)) {
                List<Path> paths = stream.collect(Collectors.toList());
                boolean exists = Files.exists(target);
                if (!exists) {
                    Files.createDirectory(target);
                }
                for (Path path : paths) {
                    SftpPath remotePath = fs.getDefaultDir().resolve(target.resolve(path.getFileName().toString()));
                    if (Files.isDirectory(path)) {
                        upload(path, remotePath, fs);
                    } else {
                        Files.copy(path, remotePath);
                    }
                }
            }
        } else {
            copyFile(source, target, fs);
        }
    }

    private void download(Path source, Path target, SftpFileSystem fs) throws IOException {
        if (Files.isDirectory(source)) {
            try (Stream<Path> stream = Files.list(fs.getDefaultDir().resolve(source))) {
                List<Path> paths = stream.collect(Collectors.toList());
                boolean exists = Files.exists(target);
                if (!exists) {
                    Files.createDirectory(target);
                }
                for (Path path : paths) {
                    Path targetFile = Paths.get(target.toFile().getAbsolutePath(), path.getFileName().toString());
                    if (Files.isDirectory(path)) {
                        download(path, targetFile, fs);
                    } else {
                        Files.copy(path, targetFile);
                    }
                }
            }
        } else {
            copyFile(source, target, fs);
        }
    }

    private void createDirectories(Path path) throws IOException {
        boolean exists = Files.exists(path);
        if (!exists) {
            if (Files.isDirectory(path)) {
                Files.createDirectories(path);
            } else {
                Path parent = path.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            }
        }
    }

    private void copyFile(Path source, Path target, SftpFileSystem fs) throws IOException {
        if (Files.isDirectory(target)) {
            target = fs.getDefaultDir().resolve(target.resolve(source.getFileName().toString()));
        }
        Files.deleteIfExists(target);
        Files.copy(source, target);
    }

    @Override
    public void close() {
        if (sshClient != null) {
            sshClient.stop();
            sshClient = null;
        }
    }
}
