package qingzhou.command;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 命令行交互输入：提示统一走 stderr，stdout 只留命令结果。
 * 无终端时降级为从 stdin 逐行读取；口令降级不隐藏输入，故在口令提示前告警。
 */
public final class ConsoleInput {
    // System.in 是进程级输入，不能 close（关闭后后续读取全部失败）；复用同一个 reader，
    // 避免每次新建 BufferedReader 预读走后续行，导致多次交互丢输入
    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private static boolean stdinWarned;

    private ConsoleInput() {
    }

    public static String readLine(String prompt) {
        Console console = System.console();
        System.err.print(prompt);
        return console == null ? readStdin() : console.readLine();
    }

    // 返回 null 表示 EOF，调用方须用后清零
    public static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console == null && !stdinWarned) {
            stdinWarned = true;
            System.err.println("\n!!! WARNING: no interactive terminal; password input is NOT hidden and may be echoed or logged !!!\n");
        }
        System.err.print(prompt);
        if (console != null) return console.readPassword();
        String line = readStdin();
        return line == null ? null : line.toCharArray();
    }

    private static String readStdin() {
        try {
            return STDIN.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
