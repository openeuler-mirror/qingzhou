package qingzhou.ssh;

public interface SSHSession extends AutoCloseable {
    void execCmd(String cmd) throws Exception;

    SSHResult execShell(String cmd) throws Exception;

    String uploadFile(String src, String dist) throws Exception;

    @Override
    void close() throws Exception;
}
