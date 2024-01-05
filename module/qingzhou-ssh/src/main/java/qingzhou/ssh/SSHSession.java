package qingzhou.ssh;

public interface SSHSession extends AutoCloseable{

    SSHResult execCmd(String cmd) throws Exception;

    String uploadFile(String src, String dist) throws Exception;

    String downLoadFile(String src, String dist) throws Exception;

    @Override
    void close();
}
