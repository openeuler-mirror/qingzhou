package qingzhou.command.cmd;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import qingzhou.command.Processor;
import qingzhou.crypto.impl.CryptoImpl;

/**
 * 生成与登录校验同源的口令摘要，输出可原样写入 qingzhou-auth.password。
 * 明文只从终端或 stdin 读：命令行参数会留在 shell 历史与进程列表。
 */
public class Digest extends Processor {
    @Override
    public String name() {
        return "digest";
    }

    @Override
    public String info() {
        return "Digest a plaintext for qingzhou-auth.password. Usage: digest (text from terminal or stdin)";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        if (args.length > 0) throw new IllegalArgumentException("plaintext must come from terminal or stdin, not args");

        char[] plaintext = readPlaintext();
        if (plaintext == null) return; // EOF/Ctrl-D
        try {
            log(new CryptoImpl().getMessageDigest().digest(new String(plaintext)));
        } finally {
            Arrays.fill(plaintext, '\0'); // 明文不驻留内存
        }
    }

    private char[] readPlaintext() throws IOException {
        Console console = System.console();
        if (console == null) { // 无终端时适配管道/重定向输入
            String line = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            return line == null ? null : line.toCharArray();
        }
        System.err.print("Text: "); // 提示走 stderr，stdout 只留摘要便于直接写入配置
        return console.readPassword();
    }
}
