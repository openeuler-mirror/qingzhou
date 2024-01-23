package qingzhou.ssh.impl;

import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import qingzhou.ssh.LifecycleListener;
import qingzhou.ssh.SSHResult;
import qingzhou.ssh.SSHSession;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SSHSessionImpl implements SSHSession {
    private final ClientSession clientSession;
    private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

    public SSHSessionImpl(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @Override
    public void addSessionListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public SSHResult execCmd(String cmd) throws Exception {
        try (ChannelExec channel = clientSession.createExecChannel(cmd);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream error = new ByteArrayOutputStream()) {
            channel.setupSensibleDefaultPty();
            channel.setOut(out);
            channel.setErr(error);
            channel.open().verify(10000L);
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 600000L);
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
                    System.err.printf("Failed to execute command: %s, exit status: %d, details: %s%n", cmd, exit, msg);
                }
            }

            SSHResultImp sshResult = new SSHResultImp();
            sshResult.setCode(exit);
            sshResult.setMessage(msg);
            return sshResult;
        }
    }

    @Override
    public SSHResult execCmdAsLogin(String cmd) throws Exception {
        return execCmd("source /etc/profile; source ~/.bash_profile; " + cmd);
    }

    @Override
    // src: 本地路径；dist: 远端路径
    public void uploadFile(String src, String dist) throws Exception {
        try (SftpFileSystem fs = SftpClientFactory.instance().createSftpFileSystem(clientSession)) {
            Path local = Paths.get(src);
            Path remote = fs.getDefaultDir().resolve(dist);
            transferFile(local, remote, new PathSupplying() {
                @Override
                public List<Path> listSrc(Path path) throws Exception {
                    try (Stream<Path> list = Files.list(path)) {
                        return list.collect(Collectors.toList());
                    }
                }

                @Override
                public Path resolveDist(Path dist) {
                    return fs.getDefaultDir().resolve(dist);
                }
            });
        }
    }

    @Override
    // src: 远端路径；dist: 本地路径
    public void downloadFile(String src, String dist) throws Exception {
        try (SftpFileSystem fs = SftpClientFactory.instance().createSftpFileSystem(clientSession)) {
            Path remote = fs.getDefaultDir().resolve(src);
            Path local = Paths.get(dist);
            transferFile(remote, local, new PathSupplying() {
                @Override
                public List<Path> listSrc(Path path) throws Exception {
                    try (Stream<Path> list = Files.list(fs.getDefaultDir().resolve(path))) {
                        return list.collect(Collectors.toList());
                    }
                }

                @Override
                public Path resolveDist(Path dist) {
                    return dist;
                }
            });
        }
    }

    private void transferFile(Path srcPath, Path distPath, PathSupplying pathSupplying) throws Exception {
        if (!Files.exists(distPath)) {
            if (Files.isDirectory(distPath)) {
                Files.createDirectories(distPath);
            } else {
                Path parent = distPath.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            }
        }

        if (Files.isDirectory(srcPath)) {
            for (Path src : pathSupplying.listSrc(srcPath)) {
                Path dist = pathSupplying.resolveDist(distPath.resolve(src.getFileName().toString()));
                transferFile(src, dist, pathSupplying);
            }

        } else {
            Files.deleteIfExists(distPath);
            Files.copy(srcPath, distPath);
        }
    }

    private interface PathSupplying {
        List<Path> listSrc(Path src) throws Exception;

        Path resolveDist(Path dist);
    }

    @Override
    public void close() throws Exception {
        closeInternal();

        lifecycleListeners.forEach(LifecycleListener::closed);
    }

    public void closeInternal() throws Exception {
        if (clientSession != null) {
            clientSession.close();
        }
    }
}
