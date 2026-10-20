package qingzhou.command.cmd.init;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import qingzhou.command.Processor;

public class Init extends Processor {
    private File instanceDir;
    private File propsFile;
    private Console console;

    private URLClassLoader cryptoLoader;
    private Object crypto;

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String info() {
        return "Initialize the instance: global key, admin password and SSL certificate.";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        instanceDir = initInstance(args);
        if (instanceDir == null) return;

        console = System.console();
        if (console == null) { // 确认与密码均需交互，无终端时不得按默认值擅自初始化
            log("no interactive terminal found, run this command in a terminal");
            return;
        }
        propsFile = new File(instanceDir, "conf/qingzhou.properties");

        URL cryptoJar = Paths.get(getLibDir().getAbsolutePath(), "components", "qingzhou-crypto.jar").toUri().toURL();
        cryptoLoader = new URLClassLoader(new URL[]{cryptoJar});
        try {
            crypto = cryptoLoader.loadClass("qingzhou.crypto.impl.CryptoImpl").getDeclaredConstructor().newInstance();
            // CryptoImpl 的密钥文件路径由 @Activate 注入，脱离 OSGi 时须显式初始化，否则 getGlobalCipher 读取的路径为空
            crypto.getClass().getMethod("init", Map.class)
                    .invoke(crypto, Collections.singletonMap("qingzhou.instance", instanceDir.getAbsolutePath()));

            new GlobalKeyStep().init(this);
            new AdminPasswordStep().init(this);
            new SslStep().init(this);
        } finally {
            cryptoLoader.close();
        }
        log("Done, restart the instance to take effect.");
    }

    File confFile(String name) {
        return new File(instanceDir, "conf/" + name);
    }

    String generateCipherKey() throws Exception {
        return (String) crypto.getClass().getMethod("generateCipherKey").invoke(crypto);
    }

    String digest(String text) throws Exception {
        Object messageDigest = crypto.getClass().getMethod("getMessageDigest").invoke(crypto);
        Method digest = cryptoLoader.loadClass("qingzhou.crypto.MessageDigest").getMethod("digest", String.class);
        return (String) digest.invoke(messageDigest, text);
    }

    String encrypt(String plain) throws Exception {
        Object globalCipher = crypto.getClass().getMethod("getGlobalCipher").invoke(crypto);
        Method encrypt = cryptoLoader.loadClass("qingzhou.crypto.Cipher").getMethod("encrypt", String.class);
        return (String) encrypt.invoke(globalCipher, plain);
    }

    String readPassword(String prompt) {
        char[] password = null;
        char[] confirm = null;
        try {
            password = console.readPassword(prompt);
            if (password == null || password.length == 0) return null; // 空口令视为跳过

            confirm = console.readPassword("Confirm password: ");
            boolean matched = confirm != null && Arrays.equals(password, confirm);
            if (!matched) {
                log("Passwords do not match, skipped.");
                return null;
            }

            return new String(password);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirm != null) Arrays.fill(confirm, '\0');
        }
    }

    boolean noConfirm(String prompt) {
        String answer = console.readLine(prompt);
        return answer == null || !answer.trim().toLowerCase(Locale.ROOT).startsWith("y"); // 默认否，回车即跳过
    }

    String readProps() throws IOException {
        return read(propsFile);
    }

    void writeProps(String content) throws IOException {
        write(propsFile, content);
    }

    static String read(File file) throws IOException {
        return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "";
    }

    static void write(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    static boolean hasValue(String content, String key) {
        for (String line : content.split("\n", -1)) {
            String trim = line.trim();
            if (trim.startsWith(key + "=")) return !trim.substring(key.length() + 1).trim().isEmpty();
        }
        return false; // 无该配置项或有但为空，均视为未初始化
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
}
