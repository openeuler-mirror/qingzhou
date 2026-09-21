package qingzhou.command.cmd;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import qingzhou.command.Processor;

public class InitInstance extends Processor {
    private static final String PROPS = "conf/qingzhou.properties";
    private static final String SECRET_PROPS = "conf/secret-key.properties";
    private static final String KEYSTORE = "conf/keystore.p12";

    private File instanceDir;
    private File propsFile;
    private URLClassLoader cryptoLoader;
    private Object crypto;

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String info() {
        return "Initialize the instance security configuration: cipher key, key pair, SSL certificate and admin password.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        String instance = args.length > 0 && !args[0].isEmpty() ? args[0] : "default";
        instanceDir = initInstance(instance);
        if (instanceDir == null) return;

        Console console = System.console();
        if (console == null) { // 确认与密码均需交互，无终端时不得按默认值擅自初始化
            logSimple("no interactive terminal found, run this command in a terminal");
            return;
        }

        propsFile = new File(instanceDir, PROPS);
        // 全局密钥的读取位置由 qingzhou.instance 决定，命令进程须显式指向目标实例
        System.setProperty("qingzhou.instance", instanceDir.getAbsolutePath());
        try {
            loadCrypto();
            initKeys();
            initSsl(console);
            initAdminPassword(console);
        } finally {
            if (cryptoLoader != null) cryptoLoader.close();
        }
        logSimple("done, restart the instance to take effect");
    }

    private void initKeys() throws Exception {
        File secretFile = new File(instanceDir, SECRET_PROPS);
        String secret = read(secretFile);
        if (isBlank(getValue(secret, "global"))) { // 已存在则静默跳过，避免已签发的令牌失效
            write(secretFile, putValue(secret, "global", generateKey()));
            logSimple("cipher key generated");
        }

        String props = read(propsFile);
        if (isBlank(getValue(props, "qingzhou-registry.private_key"))) { // 已存在则视为密钥对完整，静默跳过
            String[] pairKey = generatePairKey();
            props = putValue(props, "qingzhou-registry.private_key", encrypt(pairKey[1]));
            props = putValue(props, "qingzhou-agent.public_key", pairKey[0]);
            write(propsFile, props);
            logSimple("key pair generated");
        }
    }

    private void initSsl(Console console) throws Exception {
        if (!confirm(console, "Generate a new SSL certificate and overwrite the current one? [y/N] ")) return;

        String password = readPassword(console, "New keystore password: ");
        if (password == null) return;

        File keystore = new File(instanceDir, KEYSTORE);
        Files.deleteIfExists(keystore.toPath());
        genKeystore(keystore, password);
        String props = read(propsFile);
        props = putValue(props, "qingzhou-http-server.ssl_keystore_password", encrypt(password));
        props = putValue(props, "qingzhou-http-server.ssl_enabled", "true"); // 证书与口令同时就绪，随即启用 https
        write(propsFile, props);
        chmod600(keystore);
        logSimple("ssl certificate generated and ssl_enabled set to true, restart to enable https");
    }

    private void initAdminPassword(Console console) throws Exception {
        if (!confirm(console, "Reset the admin password? [y/N] ")) return;

        String password = readPassword(console, "New admin password: ");
        if (password == null) return;

        Object messageDigest = crypto.getClass().getMethod("getMessageDigest").invoke(crypto);
        Method digest = cryptoLoader.loadClass("qingzhou.crypto.MessageDigest")
                .getMethod("digest", String.class, String.class, int.class, int.class);
        write(propsFile, putValue(read(propsFile), "qingzhou-auth.password",
                (String) digest.invoke(messageDigest, password, "SHA-256", 16, 2)));
        logSimple("admin password updated");
    }

    private void genKeystore(File keystore, String password) throws Exception {
        List<String> command = Arrays.asList(keytool(), "-genkeypair", "-alias", "qingzhou",
                "-keyalg", "RSA", "-keysize", "3072", "-sigalg", "SHA256withRSA", "-validity", "3650",
                "-storetype", "PKCS12", "-keystore", keystore.getAbsolutePath(),
                "-storepass", password, "-keypass", password,
                "-dname", "CN=qingzhou", "-ext", "SAN=dns:localhost,ip:127.0.0.1");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (InputStream inputStream = process.getInputStream()) {
            byte[] bytes = new byte[1024 * 4];
            StringBuilder output = new StringBuilder();
            for (int n; (n = inputStream.read(bytes)) != -1; ) {
                output.append(new String(bytes, 0, n, StandardCharsets.UTF_8));
            }
            if (process.waitFor() != 0) {
                throw new IllegalStateException("keytool failed: " + output);
            }
        }
    }

    private String keytool() {
        Path bin = Paths.get(System.getProperty("java.home"), "bin");
        Path keytool = Paths.get(bin.toString(), "keytool");
        if (!keytool.toFile().isFile()) {
            keytool = Paths.get(bin.toString(), "keytool.exe");
            if (!keytool.toFile().isFile()) {
                throw new IllegalStateException("keytool not found, a JDK is required: " + bin);
            }
        }
        return keytool.toString();
    }

    private String readPassword(Console console, String prompt) {
        while (true) { // 密码不回显，输入两遍比对，避免输错后到下次登录/启动才暴露
            char[] password = console.readPassword(prompt);
            if (password == null || password.length == 0) {
                logSimple("empty password, skipped");
                return null;
            }
            char[] confirm = console.readPassword("Confirm password: ");
            if (confirm == null || confirm.length == 0) {
                logSimple("empty password, skipped");
                return null;
            }
            if (Arrays.equals(password, confirm)) return new String(password);
            logSimple("passwords do not match, try again");
        }
    }

    private boolean confirm(Console console, String prompt) {
        String answer = console.readLine(prompt);
        return answer != null && answer.trim().toLowerCase(Locale.ROOT).startsWith("y"); // 默认否，回车即跳过
    }

    private void loadCrypto() throws Exception {
        URL jarUrl = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        cryptoLoader = new URLClassLoader(new URL[]{jarUrl});
        crypto = cryptoLoader.loadClass("qingzhou.crypto.impl.CryptoImpl").getDeclaredConstructor().newInstance();
    }

    private String generateKey() throws Exception {
        return (String) crypto.getClass().getMethod("generateKey").invoke(crypto);
    }

    private String[] generatePairKey() throws Exception {
        return (String[]) crypto.getClass().getMethod("generatePairKey").invoke(crypto);
    }

    private String encrypt(String plain) throws Exception {
        Object globalCipher = crypto.getClass().getMethod("getGlobalCipher").invoke(crypto);
        Method encrypt = cryptoLoader.loadClass("qingzhou.crypto.Cipher").getMethod("encrypt", String.class);
        return (String) encrypt.invoke(globalCipher, plain);
    }

    private static void chmod600(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"));
        } catch (Exception ignored) { // Windows 等无 POSIX 权限模型的平台跳过
        }
    }

    private static String read(File file) throws IOException {
        return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
    }

    private static void write(File file, String content) throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp"); // 先写临时文件，避免写坏原文件
        Files.write(temp.toPath(), content.getBytes(StandardCharsets.UTF_8));
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    static String getValue(String content, String key) {
        for (String line : content.split("\n", -1)) {
            String trim = line.trim();
            if (trim.startsWith(key + "=")) return trim.substring(key.length() + 1).trim();
        }
        return null; // 无该配置项返回 null，有但为空返回空串
    }

    static String putValue(String content, String key, String value) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith(key + "=")) {
                lines[i] = key + "=" + value; // 有该配置项则整行替换，其余行与注释原样保留
                return String.join("\n", lines);
            }
        }
        return content + (content.isEmpty() || content.endsWith("\n") ? "" : "\n") + key + "=" + value + "\n";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
