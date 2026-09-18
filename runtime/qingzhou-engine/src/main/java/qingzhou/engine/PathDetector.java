package qingzhou.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PathDetector {
    private final String featureFiles;
    private final String detectionEnvVars;
    private String scanRoots;
    private final String processNames;

    PathDetector(Dictionary<String, String> attributes) {
        this.featureFiles = attributes.get("Qingzhou-Detection-Feature-Files");
        this.detectionEnvVars = attributes.get("Qingzhou-Detection-Env-Vars");
        this.scanRoots = attributes.get("Qingzhou-Detection-Scan-Roots");
        if (this.scanRoots == null) {
            this.scanRoots = System.getProperty("Qingzhou-Detection-Scan-Roots");
        }
        this.processNames = attributes.get("Qingzhou-Detection-Process-Names");
    }

    // ================= 主探测逻辑 =================
    public String detect() {
        String detected = detectByFile();

        if (detected == null) detected = detectByProcess();

        return detected;
    }

    private boolean matchesFeatureFile(String dir) {
        if (dir == null || dir.isEmpty() || featureFiles == null) return false;
        return Arrays.stream(featureFiles.split(",")).anyMatch(f -> !f.isEmpty() && Files.exists(Paths.get(dir, f)));
    }

    private String detectByFile() {
        List<File> detectPaths = new ArrayList<>();

        // 1. 环境变量直接指定的路径
        if (detectionEnvVars != null) {
            Arrays.stream(detectionEnvVars.split(","))
                    .map(System::getenv)
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .map(s -> new File(s.trim()))
                    .forEach(detectPaths::add);
        }

        // 2. 常见根目录的子目录扫描
        if (scanRoots != null) {
            for (String s : scanRoots.split(",")) {
                s = s.trim();
                if (s.isEmpty()) continue;
                File file = new File(s);
                if (!file.isDirectory()) continue;

                detectPaths.add(file); // 直接配置了软件的安装目录

                File[] files = file.listFiles();
                if (files != null) {
                    for (File sub : files) {
                        if (sub.isDirectory()) {
                            detectPaths.add(sub);
                        }
                    }
                }
            }
        }

        // 3. 合并流：统一用特征文件校验，找到第一个有效路径立即返回 (短路)
        for (File detectPath : detectPaths) {
            String targetPath = detectPath.getAbsolutePath();
            if (matchesFeatureFile(targetPath)) {
                return targetPath;
            }
        }
        return null;
    }

    // 通过扫描系统进程，反向推断软件的安装目录
    private String detectByProcess() {
        if (processNames == null) return null; // 未配置进程名，不做进程探测

        Process process = null;
        BufferedReader reader = null;
        try {
            // 1. 构造全平台兼容的极简原生命令
            String[] cmd;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows: 使用 PowerShell 获取所有进程的可执行文件路径（wmic 已在新版 Windows 中被移除）
                // -ErrorAction SilentlyContinue：访问受保护进程的 Path 会被拒绝，避免这类错误混入输出
                cmd = new String[]{"powershell", "-NoProfile", "-Command",
                        "Get-Process | Select-Object -ExpandProperty Path -ErrorAction SilentlyContinue"};
            } else {
                // Linux & Mac: 使用 ps 获取所有进程的启动命令及参数（-ww 避免 macOS 长命令行被截断，Linux procps 亦兼容）
                cmd = new String[]{"sh", "-c", "ps -ww -e -o args="};
            }

            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 2. 数据探查
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> { // 从行中提取匹配到的目录前缀 (到关键字为止)
                        // 统一转为小写查找，兼容大小写
                        String lowerPath = line.toLowerCase();
                        String lowerKeyword = processNames.toLowerCase();
                        for (String kw : lowerKeyword.split(",")) {
                            int idx = lowerPath.indexOf(kw);
                            if (idx != -1) {
                                return line.substring(0, idx).replace('\\', '/');
                            }
                        }
                        return null;
                    })
                    // 验证特征文件是否存在，一旦 true，findFirst 立即中断进程读取
                    .filter(this::matchesFeatureFile)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            if (process != null) {
                process.destroyForcibly();
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
