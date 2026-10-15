package qingzhou.command.cmd;

import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;

import qingzhou.command.Processor;

public class Keystore extends Processor {
    private static final char[] PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    @Override
    public String name() {
        return "keystore";
    }

    @Override
    public String info() {
        return "Generate a self-signed TLS keystore for the instance with a random password written back to qingzhou.properties.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        File instanceDir = initInstance(args);
        if (instanceDir == null) return;

        File confDir = new File(instanceDir, "conf");
        File keystore = new File(confDir, "keystore.p12");
        String password = randomPassword();
        Files.deleteIfExists(keystore.toPath()); // 不能预创建空文件：JDK8 keytool 对已存在的空文件报错
        InitInstance.genKeystore(keystore, password);
        InitInstance.chmod600(keystore); // keytool 生成后立即收敛权限

        File propsFile = new File(confDir, "qingzhou.properties");
        InitInstance.write(propsFile, InitInstance.putValue(InitInstance.read(propsFile),
                "qingzhou-http-server.ssl_keystore_password", "plain:" + password));
        log("keystore generated: " + keystore);
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            password.append(PASSWORD_CHARS[random.nextInt(PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }
}
