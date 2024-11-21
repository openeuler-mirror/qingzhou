package qingzhou.ssh;

public interface SSHSession extends AutoCloseable {
    SSHResult execCmd(String cmd) throws Exception;

    SSHResult execCmdAsLogin(String cmd) throws Exception;

    // src: 本地路径；dist: 远端路径
    void uploadFile(String src, String dist) throws Exception;

    // src: 远端路径；dist: 本地路径
    void downloadFile(String src, String dist) throws Exception;

    @Override
    void close() throws Exception;
}
