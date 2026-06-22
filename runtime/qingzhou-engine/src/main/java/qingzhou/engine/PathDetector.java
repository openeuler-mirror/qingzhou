package qingzhou.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Objects;
import java.util.stream.Stream;

public class PathDetector {
    private final String featureFiles;
    private final String detectionEnvVars;
    private final String scanRoots;
    private final String processNames;

    PathDetector(Dictionary<String, String> attributes) {
        this.featureFiles = attributes.get("Qingzhou-Detection-Feature-Files");

        this.detectionEnvVars = attributes.get("Qingzhou-Detection-Env-Vars");

        String scanRootsStr = attributes.get("Qingzhou-Detection-Scan-Roots");
        if (scanRootsStr == null) {
            scanRootsStr = System.getProperty("Qingzhou-Detection-Scan-Roots");
        }
        this.scanRoots = scanRootsStr;

        this.processNames = attributes.get("Qingzhou-Detection-Process-Names");
    }

    // ================= 主探测逻辑 =================
    public String detect() {
        String detected = detectByFile();

        if (detected == null) detected = detectByProcess();

        return detected;
    }

    private boolean matchesFeatureFile(String dir) {
        if (dir == null || dir.isEmpty()) return false;
        return Arrays.stream(featureFiles.split(",")).anyMatch(f -> !f.isEmpty() && Files.exists(Paths.get(dir, f)));
    }

    private String detectByFile() {
        // 1. 环境变量直接指定的路径
        Stream<String> envPaths = Stream.empty();
        if (detectionEnvVars != null) {
            envPaths = Arrays.stream(detectionEnvVars.split(",")).map(System::getenv).filter(p -> p != null && !p.isEmpty());
        }

        // 2. 常见根目录的子目录扫描
        Stream<String> scanPaths = Stream.empty();
        if (scanRoots != null) {
            scanPaths = Arrays.stream(scanRoots.split(","))
                    .filter(r -> new File(r).isDirectory())
                    .flatMap(r -> Arrays.stream(Objects.requireNonNull(new File(r).listFiles())))
                    .filter(File::isDirectory)
                    .map(File::getAbsolutePath);
        }

        // 3. 合并流：统一用特征文件校验，找到第一个有效路径立即返回 (短路)
        return Stream.concat(envPaths, scanPaths)
                .filter(this::matchesFeatureFile)
                .findFirst()
                .orElse(null);
    }

    // 通过扫描系统进程，反向推断软件的安装目录
    private String detectByProcess() {
        Process process = null;
        try {
            // 1. 构造全平台兼容的极简原生命令
            String[] cmd;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows: 使用 wmic 获取所有进程的可执行文件路径
                cmd = new String[]{"cmd", "/c", "wmic process where \"ExecutablePath is not null\" get ExecutablePath /format:list"};
            } else {
                // Linux & Mac: 使用 ps 获取所有进程的启动命令及参数 (通用性最强)
                cmd = new String[]{"sh", "-c", "ps -e -o args="};
            }

            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // 2. 数据探查
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> { // 按空格分隔后，取第一个（即进程的可执行文件路径或命令本身）
                        return line.trim().split("\\s+")[0];
                    })
                    .map(line -> { // Windows wmic 输出格式是 "ExecutablePath=C:\xxx"，截取等号后的内容
                        if (line.contains("=")) {
                            line = line.substring(line.indexOf('=') + 1);
                        }
                        return line;
                    })
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
            e.printStackTrace();
            return null;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }
}
