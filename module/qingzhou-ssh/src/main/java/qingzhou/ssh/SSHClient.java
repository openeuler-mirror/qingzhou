package qingzhou.ssh;

public interface SSHClient extends AutoCloseable {
    /**
     * SSH identifier for RSA keys
     */
    String SSH_RSA = "ssh-rsa";
    /**
     * SSH identifier for DSA keys
     */
    String SSH_DSS = "ssh-dss";

    /**
     * SSH identifier for ED25519 elliptic curve keys
     */
    String SSH_ED25519 = "ssh-ed25519";

    SSHSession getSSHSession() throws Exception;

    SSHResult execCmd(String cmd) throws Exception;

    SSHResult execShell(String cmd) throws Exception;

    String uploadFile(String src, String dist) throws Exception;

    String downLoadFile(String src, String dist) throws Exception;

    @Override
    void close() throws Exception;
}
