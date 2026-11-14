package qingzhou.app.nginx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 平台相关命令的统一执行入口：集中处理超时、输出回收与进程存活判断，避免各模型重复拼装。
 */
final class NginxCommand {
    // nginx 正常输出很小；异常配置可能刷屏，超过上限直接丢弃，防止一次请求占满堆内存
    private static final int MAX_OUTPUT_BYTES = 512 * 1024;
    private static final long WAIT_AFTER_DESTROY_SECONDS = 2;

    private NginxCommand() {
    }

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    static Result run(Path workDir, long timeoutSeconds, String... command) {
        return run(workDir, timeoutSeconds, Arrays.asList(command));
    }

    static Result run(Path workDir, long timeoutSeconds, List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
        if (workDir != null) builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IllegalStateException("无法执行命令: " + command.get(0), e);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> copy(process.getInputStream(), output), "nginx-command-reader");
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("命令执行被中断: " + command.get(0), e);
        }

        if (!finished) {
            process.destroyForcibly();
            try {
                process.waitFor(WAIT_AFTER_DESTROY_SECONDS, TimeUnit.SECONDS);
                reader.join(TimeUnit.SECONDS.toMillis(WAIT_AFTER_DESTROY_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            reader.join(TimeUnit.SECONDS.toMillis(WAIT_AFTER_DESTROY_SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String text = new String(output.toByteArray(), Charset.defaultCharset());
        return new Result(finished ? process.exitValue() : -1, text, !finished);
    }

    private static void copy(InputStream in, ByteArrayOutputStream output) {
        byte[] buffer = new byte[4096];
        try (InputStream input = in) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                int remaining = MAX_OUTPUT_BYTES - output.size();
                if (remaining > 0) output.write(buffer, 0, Math.min(read, remaining));
            }
        } catch (IOException ignored) {
            // 进程被强杀时流会提前关闭，输出保留已读取部分即可
        }
    }

    /**
     * kill -0 在无权限时也返回非 0，但进程实际存在；把该场景判为存活，避免误报 nginx 已停止。
     */
    static boolean isProcessAlive(int pid) {
        if (pid <= 0) return false;
        if (isWindows()) {
            Result result = run(null, 5, "tasklist", "/FI", "PID eq " + pid, "/NH");
            return result.getOutput().contains(String.valueOf(pid));
        }
        Result result = run(null, 5, "kill", "-0", String.valueOf(pid));
        if (result.isSuccess()) return true;
        String output = result.getOutput().toLowerCase(Locale.ROOT);
        return output.contains("not permitted") || output.contains("permission denied");
    }

    static String prefixArg(Path prefix) {
        String path = prefix.toAbsolutePath().normalize().toString();
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    static final class Result {
        private final int exitCode;
        private final String output;
        private final boolean timedOut;

        Result(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output;
            this.timedOut = timedOut;
        }

        boolean isSuccess() {
            return !timedOut && exitCode == 0;
        }

        String getOutput() {
            return output == null ? "" : output.trim();
        }

        String toMessage() {
            if (timedOut) return "命令执行超时";
            String text = getOutput();
            return text.isEmpty() ? "退出码 " + exitCode : text;
        }
    }
}
