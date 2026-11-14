package qingzhou.app.nginx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * nginx 进程的启停、配置校验与状态采集。
 *
 * 所有写操作串行执行：同一时刻只允许一个 start/stop/reload，避免并发请求把实例状态搅乱。
 */
final class NginxService {
    static final Object PROCESS_LOCK = new Object();
    private static final long COMMAND_TIMEOUT_SECONDS = 20;
    private static final long START_WAIT_SECONDS = 8;
    private static final long STOP_WAIT_SECONDS = 15;

    private final NginxInstall install;

    NginxService(NginxInstall install) {
        this.install = install;
    }

    NginxInstall getInstall() {
        return install;
    }

    NginxCommand.Result test() {
        synchronized (PROCESS_LOCK) {
            return exec("-t");
        }
    }

    void start() {
        synchronized (PROCESS_LOCK) {
            if (isRunning()) {
                throw new IllegalStateException("nginx 已在运行，PID " + pidOrZero());
            }
            if (install.isDaemonOff()) {
                throw new IllegalStateException("配置包含 daemon off，应用无法以后台方式启动 nginx");
            }
            NginxCommand.Result result = exec();
            if (!result.isSuccess()) {
                throw new IllegalStateException("nginx 启动失败: " + result.toMessage());
            }
            if (!waitForRunning(true, START_WAIT_SECONDS)) {
                throw new IllegalStateException("nginx 启动命令已执行，但未检测到运行中的进程。"
                        + "请检查配置文件与错误日志: " + result.toMessage());
            }
        }
    }

    void stop(boolean force) {
        synchronized (PROCESS_LOCK) {
            if (!isRunning()) {
                throw new IllegalStateException("nginx 未运行");
            }
            requireNginxProcess();
            NginxCommand.Result result = exec("-s", force ? "stop" : "quit");
            if (!result.isSuccess()) {
                throw new IllegalStateException("nginx 停止失败: " + result.toMessage());
            }
            if (!waitForRunning(false, STOP_WAIT_SECONDS)) {
                throw new IllegalStateException("nginx 未在 " + STOP_WAIT_SECONDS
                        + " 秒内退出，请稍后重试或使用强制停止");
            }
        }
    }

    void reload() {
        synchronized (PROCESS_LOCK) {
            if (!isRunning()) {
                throw new IllegalStateException("nginx 未运行，无法重载");
            }
            NginxCommand.Result test = exec("-t");
            if (!test.isSuccess()) {
                throw new IllegalStateException("配置校验失败，未重载: " + test.toMessage());
            }
            requireNginxProcess();
            NginxCommand.Result result = exec("-s", "reload");
            if (!result.isSuccess()) {
                throw new IllegalStateException("nginx 重载失败: " + result.toMessage());
            }
        }
    }

    void restart() {
        synchronized (PROCESS_LOCK) {
            if (!isRunning()) {
                throw new IllegalStateException("nginx 未运行，无法重启");
            }
            NginxCommand.Result test = exec("-t");
            if (!test.isSuccess()) {
                throw new IllegalStateException("配置校验失败，未重启: " + test.toMessage());
            }
            if (install.isDaemonOff()) {
                throw new IllegalStateException("配置包含 daemon off，应用无法以后台方式重启 nginx");
            }
            stop(false);
            start();
        }
    }

    boolean isRunning() {
        int pid = pidOrZero();
        return pid > 0 && isNginxMaster(pid);
    }

    /**
     * pid 文件可能过期，PID 也可能被系统复用；发信号前确认目标进程确为 nginx master，
     * 避免以 root 运行时把 QUIT/TERM 发给无关进程。
     */
    private void requireNginxProcess() {
        int pid = pidOrZero();
        if (!isNginxMaster(pid)) {
            throw new IllegalStateException("无法确认 PID " + pid + " 对应 nginx master 进程，已拒绝发送信号");
        }
    }

    private static boolean isNginxMaster(int pid) {
        if (pid <= 0 || !NginxCommand.isProcessAlive(pid)) return false;
        String command = processCommand(pid);
        if (command == null || command.isEmpty()) return false;
        String lower = command.toLowerCase(Locale.ROOT);
        if (NginxCommand.isWindows()) return lower.contains("nginx.exe");
        return lower.contains("nginx: master process")
                || Pattern.compile("(^|/)nginx(: master process)?(\\s|$)").matcher(lower).find();
    }

    private static String processCommand(int pid) {
        if (NginxCommand.isWindows()) {
            NginxCommand.Result result = NginxCommand.run(null, 5,
                    "tasklist", "/FI", "PID eq " + pid, "/FO", "CSV", "/NH");
            if (!result.isSuccess()) return null;
            String output = result.getOutput().trim();
            int end = output.indexOf('"', 1);
            if (output.startsWith("\"") && end > 0) {
                return output.substring(1, end);
            }
            return output;
        }

        Path procCmdline = java.nio.file.Paths.get("/proc", String.valueOf(pid), "cmdline");
        if (Files.isRegularFile(procCmdline)) {
            try (InputStream input = Files.newInputStream(procCmdline)) {
                byte[] bytes = new byte[4096];
                int read = input.read(bytes);
                if (read > 0) return new String(bytes, 0, read, StandardCharsets.UTF_8).replace('\0', ' ');
            } catch (IOException ignored) {
                // /proc 不可读时回退 ps
            }
        }

        NginxCommand.Result result = NginxCommand.run(null, 5, "ps", "-p", String.valueOf(pid), "-o", "args=");
        return result.isSuccess() ? result.getOutput() : null;
    }

    int pidOrZero() {
        if (!Files.isRegularFile(install.pidFile)) return 0;
        try (InputStream input = Files.newInputStream(install.pidFile)) {
            byte[] bytes = new byte[64];
            int read = input.read(bytes);
            if (read <= 0) return 0;
            String text = new String(bytes, 0, read, StandardCharsets.UTF_8).trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NoSuchFileException e) {
            return 0; // stop 与状态查询竞态：pid 文件已被 nginx 删除
        } catch (IOException e) {
            throw new IllegalStateException("读取 nginx pid 文件失败: " + install.pidFile, e);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 返回 worker 进程数；无法采集时返回 -1，调用方应省略该指标而不是填哨兵值。
     */
    int workerCount() {
        int pid = pidOrZero();
        if (pid <= 0 || !isRunning()) return 0;

        try {
            if (NginxCommand.isWindows()) {
                String script = "@(Get-CimInstance Win32_Process -Filter \"ParentProcessId=" + pid
                        + "\" | Where-Object { $_.CommandLine -like '*worker process*' }).Count";
                NginxCommand.Result result = NginxCommand.run(install.prefix, 10,
                        "powershell", "-NoProfile", "-NonInteractive", "-Command", script);
                return result.isSuccess() ? Integer.parseInt(result.getOutput().trim()) : -1;
            }

            NginxCommand.Result result = NginxCommand.run(install.prefix, 10,
                    "ps", "-ww", "-e", "-o", "pid=,ppid=,args=");
            if (!result.isSuccess()) return -1;

            int workers = 0;
            for (String line : result.getOutput().split("\\R")) {
                String[] parts = line.trim().split("\\s+", 3);
                if (parts.length == 3 && String.valueOf(pid).equals(parts[1])
                        && parts[2].contains("nginx: worker process")) {
                    workers++;
                }
            }
            return workers;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    String statusText() {
        return isRunning() ? "running" : "stopped";
    }

    String version() {
        if (install.version != null && !install.version.isEmpty()) return install.version;
        NginxCommand.Result result = exec("-v");
        String text = result.getOutput();
        int index = text.indexOf("nginx/");
        if (index < 0) return "";
        int end = index + "nginx/".length();
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        return text.substring(index + "nginx/".length(), end);
    }

    private NginxCommand.Result exec(String... extra) {
        List<String> command = new ArrayList<>();
        command.add(install.binary.toString());
        command.add("-p");
        command.add(NginxCommand.prefixArg(install.prefix));
        command.add("-c");
        command.add(install.conf.toString());
        command.addAll(Arrays.asList(extra));
        return NginxCommand.run(install.prefix, COMMAND_TIMEOUT_SECONDS, command);
    }

    private boolean waitForRunning(boolean expected, long seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (isRunning() == expected) return true;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待 nginx 状态时被中断", e);
            }
        }
        return isRunning() == expected;
    }
}
