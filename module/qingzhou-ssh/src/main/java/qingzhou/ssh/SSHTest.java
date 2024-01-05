package qingzhou.ssh;

import qingzhou.ssh.impl.SSHClientImpl;

import java.util.HashMap;
import java.util.Map;

public class SSHTest {
    public static void main(String[] args) throws Exception {
        cmd();
    }

    private static void cmd() {
        try {
            SSHClient client = new SSHClientImpl().start(getSSHConfig());
            SSHResult sshResult = client.execCmd("java -version");
            System.out.println(sshResult.getMessage());
            System.out.println("----------------------------------------");

            String u = client.uploadFile("D:\\TongGit\\openeuler\\qingzhou\\package\\qingzhou\\target\\qingzhou\\qingzhou\\bin\\qingzhou-launcher.jar", "/usr/xuyn");
            String d = client.downLoadFile("/usr/xuyn/nodeagent-1.0.jar", "D:\\TongGit\\openeuler\\qingzhou\\package\\qingzhou\\target\\nodeagent-1.0.jar");

            SSHSession sshSession = client.getSSHSession();
            SSHResult sshResult1 = sshSession.execCmd("java -version");
            System.out.println(sshResult1.getMessage());
            System.out.println("----------------------------------------");

            SSHResult sshResult2 = sshSession.execCmd("pwd");
            System.out.println(sshResult2.getMessage());

            String u1 = sshSession.uploadFile("D:\\TongGit\\openeuler\\qingzhou\\package\\qingzhou\\target\\qingzhou\\qingzhou\\bin\\qingzhou-launcher.jar", "/usr/xuyn");
            String d1 = sshSession.downLoadFile("/usr/xuyn/nodeagent-1.0.jar", "D:\\TongGit\\openeuler\\qingzhou\\package\\qingzhou\\target\\nodeagent-1.0.jar");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> getSSHConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("ip", "10.10.81.163");
        config.put("sshPort", "22");
        config.put("sshUserName", "root");
        config.put("sshPassword", "qazwsx.101");

        return config;
    }
}
