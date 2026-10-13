package qingzhou.command.cmd;

import java.io.*;
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
        instanceDir = initInstance(args);
        if (instanceDir == null) return;

        Console console = System.console();
        if (console == null) { // 确认与密码均需交互，无终端时不得按默认值擅自初始化
            log("no interactive terminal found, run this command in a terminal");
            return;
        }

        propsFile = new File(instanceDir, "conf/qingzhou.properties");
        try {
            loadCrypto();
            initKeys();
            initSsl(console);
            initAdminPassword(console);
        } finally {
            if (cryptoLoader != null) cryptoLoader.close();
        }
        log("done, restart the instance to take effect");
    }

    private void initKeys() throws Exception {
        File secretFile = new File(instanceDir, "conf/secret-key.properties");
        String secret = read(secretFile);
        if (isBlank(getValue(secret, "global"))) { // 已存在则静默跳过，避免已签发的令牌失效
            write(secretFile, putValue(secret, "global", generateKey()));
            log("cipher key generated");
        }

        String props = read(propsFile);
        if (isBlank(getValue(props, "qingzhou-registry.private_key"))) { // 已存在则视为密钥对完整，静默跳过
            String[] pairKey = generatePairKey();
            props = putValue(props, "qingzhou-registry.private_key", encrypt(pairKey[1]));
            props = putValue(props, "qingzhou-agent.public_key", pairKey[0]);
            write(propsFile, props);
            log("key pair generated");
        }
    }

    private void initSsl(Console console) throws Exception {
        if (!confirm(console, "Generate a new SSL certificate and overwrite the current one? [y/N] ")) return;

        String password = readPassword(console, "New keystore password: ");
        if (password == null) return;

        File keystore = new File(instanceDir, "conf/keystore.p12");
        Files.deleteIfExists(keystore.toPath());
        if (!keystore.createNewFile()) throw new IllegalStateException("cannot create keystore: " + keystore);
        chmod600(keystore); // keytool 写入前即收敛权限，避免口令文件短暂可读
        genKeystore(keystore, password);
        String props = read(propsFile);
        props = putValue(props, "qingzhou-http-server.ssl_keystore_password", encrypt(password));
        props = putValue(props, "qingzhou-http-server.ssl_enabled", "true"); // 证书与口令同时就绪，随即启用 https
        write(propsFile, props);
        chmod600(keystore);
        log("ssl certificate generated and ssl_enabled set to true, restart to enable https");
    }

    private void initAdminPassword(Console console) throws Exception {
        if (!confirm(console, "Reset the admin password? [y/N] ")) {
            log("warning: admin password was not changed");
            return;
        }

        String password = readPassword(console, "New admin password: ");
        if (password == null) return;

        Object messageDigest = crypto.getClass().getMethod("getMessageDigest").invoke(crypto);
        Method digest = cryptoLoader.loadClass("qingzhou.crypto.MessageDigest")
                .getMethod("digest", String.class, String.class, int.class, int.class);
        write(propsFile, putValue(read(propsFile), "qingzhou-auth.password",
                // PBKDF2 替代简单迭代哈希：带盐 + 高迭代次数（200000），抗离线爆破；旧 SHA-256 摘要仍可经 matches 校验
                (String) digest.invoke(messageDigest, password, "PBKDF2WithHmacSHA256", 16, 200000)));
        log("admin password updated");
    }

    private void genKeystore(File keystore, String password) throws Exception {
        List<String> command = Arrays.asList(keytool(), "-genkeypair", "-alias", "qingzhou",
                "-keyalg", "RSA", "-keysize", "3072", "-sigalg", "SHA256withRSA", "-validity", "3650",
                "-storetype", "PKCS12", "-keystore", keystore.getAbsolutePath(),
                "-dname", "CN=qingzhou", "-ext", "SAN=dns:localhost,ip:127.0.0.1");
        // 口令经 stdin 传入而非命令行参数，避免明文出现在 ps 输出中被同机其它用户读取
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder output = new StringBuilder();
        // keytool 的提示输出可能在 stdin 写入期间填满管道缓冲，须并行读取输出，避免双方互相等待死锁
        Thread outputDrainer = new Thread(() -> {
            try (InputStream inputStream = process.getInputStream()) {
                byte[] bytes = new byte[1024 * 4];
                for (int n; (n = inputStream.read(bytes)) != -1; ) {
                    output.append(new String(bytes, 0, n, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) { // 进程退出时读流中断属正常路径
            }
        });
        outputDrainer.start();
        try (OutputStream out = process.getOutputStream()) {
            // 新建密钥库依次索要 storepass（两次）与 keypass；keypass 缺省沿用 storepass，
            // 重复写入同一口令以覆盖两种提示顺序，多余行被进程忽略
            byte[] line = (password + "\n").getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < 4; i++) out.write(line);
            out.flush();
        }
        // 先等进程退出（带超时），再收拢排空线程：join 放在前面时，keytool 挂起会让排空线程
        // 永远阻塞在 read 上，下方的超时保护实际不可达
        if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) { // 挂起即强杀，避免 init 命令永久阻塞
            process.destroyForcibly();
        }
        outputDrainer.join(5_000);
        if (process.isAlive() || process.exitValue() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
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
                log("empty password, skipped");
                return null;
            }
            char[] confirm = console.readPassword("Confirm password: ");
            if (confirm == null || confirm.length == 0) {
                Arrays.fill(password, '\0');
                if (confirm != null) Arrays.fill(confirm, '\0');
                log("empty password, skipped");
                return null;
            }
            boolean matched = Arrays.equals(password, confirm);
            String value = matched ? new String(password) : null;
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
            if (matched) return value;
            log("passwords do not match, try again");
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
        chmod600(temp); // 配置可能含密钥/口令，落盘即收敛权限，不留明文窗口
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        chmod600(file);
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
                // 有该配置项则整行替换，其余行与注释原样保留；保留行尾 \r，避免 CRLF 文件被替换后行尾混杂
                String cr = lines[i].endsWith("\r") ? "\r" : "";
                lines[i] = key + "=" + value + cr;
                return String.join("\n", lines);
            }
        }
        return content + (content.isEmpty() || content.endsWith("\n") ? "" : "\n") + key + "=" + value + "\n";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
