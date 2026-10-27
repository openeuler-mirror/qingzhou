package qingzhou.command.cmd.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class SslStep {
    void init(Init init) throws Exception {
        String props = init.readProps();
        if (Init.hasValue(props, "qingzhou-http-server.ssl_keystore_password")) return; // 已有值则静默跳过
        if (init.noConfirm("Generate the SSL certificate conf/keystore.p12 and enable https? (recommended) [y/N] "))
            return;

        String password = init.readPassword("Input keystore password: ");
        File keystore = init.confFile("keystore.p12");
        Files.deleteIfExists(keystore.toPath()); // 不能预创建空文件：JDK8 keytool 对已存在的空文件报错
        boolean success = genKeystore(keystore, password, init);
        if (!success) {
            init.log("Failed to generate keystore.");
            return;
        }

        props = Init.putValue(props, "qingzhou-http-server.ssl_keystore_password", init.getCrypto().getGlobalCipher().encrypt(password));
        props = Init.putValue(props, "qingzhou-http-server.ssl_enabled", "true");
        init.writeProps(props);
        init.log("SSL certificate generated and https enabled.");
    }

    private static boolean genKeystore(File keystore, String password, Init init) throws Exception {
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
        if (!process.waitFor(60, TimeUnit.SECONDS)) { // 挂起即强杀，避免 init 命令永久阻塞
            process.destroyForcibly();
        }
        outputDrainer.join(5_000);
        if (process.isAlive() || process.exitValue() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }

        boolean success = keystore.isFile();
        if (!success) {
            init.log(output.toString()); // 密码过短，进程也会正常结束，这里可以打印出错误详情
        }
        return success;
    }

    private static String keytool() {
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
}
