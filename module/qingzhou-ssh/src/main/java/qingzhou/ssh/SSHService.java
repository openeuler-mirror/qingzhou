package qingzhou.ssh;

import java.io.IOException;
import java.util.Map;

public interface SSHService {
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

    SSHService start(Map<String, String> config);

    /**
     * 直接执行 特点 拿不到环境变量  执行系统自带的命令
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    SshResult execCmd(String cmd) throws Exception;

    /**
     * 使用shell去执行
     * 与TWSshClient#execCmd(java.lang.String) 区别是可以读取到环境变量
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    SshResult execShell(String cmd) throws Exception;

    String uploadFile(String src, String dist) throws IOException;

    String downLoadFile(String src, String dist) throws IOException;

    void close();
}
