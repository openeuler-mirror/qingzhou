package qingzhou.command.cmd.init;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import qingzhou.command.Processor;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.impl.CryptoImpl;

public class Init extends Processor {
    private File instanceDir;
    private File propsFile;
    private Console console;
    private Crypto crypto;

    @Override
    public String name() {
        return "init";
    }

    @Override
    public String info() {
        return "Initialize the instance: global key, admin password, TOTP and SSL certificate.";
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

        crypto = new CryptoImpl() {{
            init(Collections.singletonMap("qingzhou.instance", instanceDir.getAbsolutePath()));
        }};

        new GlobalKeyStep().init(this);
        new AdminPasswordStep().init(this);
        new TotpStep().init(this);
        new SslStep().init(this);
        log("Done, restart the instance to take effect.");
    }

    Crypto getCrypto() {
        return crypto;
    }

    File confFile(String name) {
        return new File(instanceDir, "conf/" + name);
    }

    // 口令不回显、输错无法自查：为空或不一致即重新输入，返回时必已确认一致；Ctrl+C 退出
    String readPassword(String prompt) {
        while (true) {
            char[] password = console.readPassword(prompt);
            char[] confirm = console.readPassword("Confirm password: ");
            // 终端关闭（EOF）时继续循环只会刷屏，直接终止
            if (password == null || confirm == null) throw new IllegalStateException("no input, aborted");

            try {
                if (password.length == 0) {
                    log("Password cannot be empty.");
                    continue;
                }
                if (Arrays.equals(password, confirm)) return new String(password);
                log("Passwords do not match, please try again.");
            } finally {
                Arrays.fill(password, '\0');
                Arrays.fill(confirm, '\0');
            }
        }
    }

    String readLine(String prompt) {
        return console.readLine(prompt);
    }

    boolean noConfirm(String prompt) {
        String answer = readLine(prompt);
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
