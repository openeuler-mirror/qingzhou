package qingzhou.detector.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 跨平台工具方法
 */
public class PlatformUtil {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isLinux() {
        return OS.contains("linux");
    }

    public static boolean isMac() {
        return OS.contains("mac") && OS.contains("os");
    }

    public static List<String> exec(String... cmd) {
        return exec(5, cmd);
    }

    /**
     * 执行命令，返回合并后的 stdout + stderr
     *
     * @param timeout
     * @param cmd
     * @return 输出内容；超时或异常返回 null
     */
    public static List<String> exec(long timeout, String... cmd) {
        String charset = System.getProperty("file.encoding", "UTF-8");
        List<String> result = new ArrayList();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<String>> future = executor.submit(() -> {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();// 合并 stderr 到 stdout
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                }
                process.waitFor();
                return result;
            }
        });

        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            future.cancel(true);
            //e.printStackTrace();
            return result;
        } finally {
            executor.shutdownNow();
        }
    }
    
    // ==================== 可执行文件定位 ====================
    /**
     * 通过系统命令定位可执行文件的完整绝对路径
     * @param exeName
     * @return 
     */
    public static String locateExecutable(String exeName) {
        if (PlatformUtil.isWindows()) {
            return locateWindows(exeName);
        } else {
            return locateUnix(exeName);
        }
    }

    /**
     * Windows: 使用 where 命令定位
     *
     * where 命令从 Vista/Server 2003 开始内置可用。
     */
    private static String locateWindows(String exeName) {
        File[] files = File.listRoots();
        for (File dir : files) {
            String[] cmd = {"where", "/r", dir.getPath(), exeName};
            List<String> lines = PlatformUtil.exec(15, cmd);
            for (String line : lines) {
                line = line.trim();
                // 过滤 "INFO: Could not find files for the given pattern(s)." 等提示
                if (line.isEmpty() || line.toLowerCase().startsWith("info:")) {
                    continue;
                }
                try {
                    if (Files.exists(Paths.get(line))) {
                        return line;
                    }
                } catch (Exception e) {
                    // 忽略无效路径格式
                }
            }

            // 备用：若原名称无 .exe 后缀，自动追加后重试
            if (!exeName.toLowerCase().endsWith(".exe")) {
                return locateWindows(exeName + ".exe");
            }
        }
        return null;
    }

    /**
     * Unix/Linux/macOS: 使用 which / type / whereis 三级降级定位
     */
    private static String locateUnix(String exeName) {
        // 1. 首选 which（几乎所有 Unix 系统都有）
        String[] cmdWhich = {"which", exeName};
        List<String> lines = PlatformUtil.exec(cmdWhich);
        if (!lines.isEmpty()) {
            String path = lines.get(0).trim();//.split("\\r?\\n")[0].trim();
            if (!path.isEmpty() && !path.startsWith("no ") && Files.exists(Paths.get(path))) {
                return path;
            }
        }

        // 2. 备用 type -P（通过 sh -c 执行，因为 type 通常是 shell 内置命令）
        // 对 exeName 做简单的单引号转义，防止注入
        String safeExe = exeName.replace("'", "'\"'\"'");
        String[] cmdType = {"sh", "-c", "type -P '" + safeExe + "'"};
        lines = PlatformUtil.exec(cmdType);
        if (!lines.isEmpty()) {
            String path = lines.get(0).trim();//output.trim().split("\\r?\\n")[0].trim();
            if (!path.isEmpty() && !path.contains("not found") && Files.exists(Paths.get(path))) {
                return path;
            }
        }

        // 3. 最后尝试 whereis -b（返回格式: nginx: /usr/sbin/nginx /usr/share/nginx ...）
        String[] cmdWhereis = {"whereis", "-b", exeName};
        lines = PlatformUtil.exec(cmdWhereis);
        if (!lines.isEmpty()) {
            String marker = exeName + ":";
            if (lines.get(0).startsWith(marker)) {// TODO ?
                String rest = lines.get(0).substring(marker.length()).trim();
                for (String part : rest.split("\\s+")) {
                    if (Files.exists(Paths.get(part))) {
                        return part;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 使用 which / where 查找可执行文件路径
     *
     * @param name
     * @return
     */
    public static Optional<java.nio.file.Path> findExecutable(String name) {
        String command = isWindows() ? "where" : "which";
        List<String> lines = exec(command, name);
        if (!lines.isEmpty()) {
            String line = lines.get(0).trim();
            if (!line.isEmpty() && !line.contains(":")) {
                return Optional.of(java.nio.file.Paths.get(line));
            }
        }
        return Optional.empty();
    }

    /**
     * 查询 Windows 注册表键值
     *
     * @param keyPath
     * @param valueName
     * @return
     */
    public static Optional<String> queryRegistry(String keyPath, String valueName) {
        if (!isWindows()) {
            return Optional.empty();
        }
        for (String line : exec("reg", "query", keyPath, "/v", valueName)) {
            if (line.contains(valueName) && !line.startsWith("ERROR")) {
                String[] parts = line.trim().split("\\s+", 4);
                if (parts.length >= 4) {
                    return Optional.of(parts[3].trim());
                }
            }
        }
        return Optional.empty();
    }
}
