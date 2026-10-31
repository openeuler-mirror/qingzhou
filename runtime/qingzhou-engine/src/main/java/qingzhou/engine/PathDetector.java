package qingzhou.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

// 由 PathDetectionScheduler 按 bundle 清单驱动；清单值在构造期一次解析，避免每拍探测重复 split
class PathDetector {
    private static final String USER_HOME_VAR = "user.home";
    // 进程清单跨 bundle 共享：全量扫描开销大，探测调度为 5 秒一次（见 PathDetectionScheduler），缓存须明显长于该周期
    private static final long SCAN_CACHE_MILLIS = 30_000;
    // 单次扫描读超时：子进程挂起时强杀，读阻塞随流关闭解除，避免探测线程被永久卡死
    private static final long SCAN_READ_TIMEOUT_MILLIS = 10_000;

    // 仅在同一 monitor 内读写，可见性由锁保证，故不加 volatile
    private static List<String> cachedScanLines;
    private static long lastScanAt;

    private final List<String> featureFileNames;
    private final List<String> envVarNames;
    private final List<String> scanRootPaths;
    private final List<String> processKeywords;

    PathDetector(Dictionary<String, String> attributes) {
        this.featureFileNames = splitCsv(attributes.get("Qingzhou-Detection-Feature-Files"));
        this.envVarNames = splitCsv(attributes.get("Qingzhou-Detection-Env-Vars"));

        List<String> roots = splitCsv(attributes.get("Qingzhou-Detection-Scan-Roots"));
        // 清单未给出时回落到 JVM 属性，便于部署期覆盖扫描范围
        this.scanRootPaths = roots.isEmpty() ? splitCsv(System.getProperty("Qingzhou-Detection-Scan-Roots")) : roots;

        List<String> keywords = new ArrayList<>();
        for (String name : splitCsv(attributes.get("Qingzhou-Detection-Process-Names"))) {
            keywords.add(name.toLowerCase(Locale.ROOT));
        }
        this.processKeywords = keywords;
    }

    String detect() throws Exception {
        String detectedPath = detectByFile();
        return detectedPath != null ? detectedPath : detectByProcess();
    }

    // 候选目录按优先级：环境变量指向的目录、扫描根目录自身及其一级子目录；流惰性求值，首个命中即短路，不再全量枚举
    private String detectByFile() {
        Stream<File> envCandidates = envVarNames.stream()
                .map(System::getenv)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(File::new);
        return Stream.concat(envCandidates, rootCandidates())
                .filter(this::hasFeatureFile)
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse(null);
    }

    private Stream<File> rootCandidates() {
        return scanRootPaths.stream()
                .map(PathDetector::resolveHomeVar)
                .map(File::new)
                .filter(File::isDirectory)
                .flatMap(root -> Stream.concat(Stream.of(root), subDirs(root)));
    }

    // 清单中以 user.home 指代当前用户主目录（见《轻舟应用开发规范》自动发现章节）
    private static String resolveHomeVar(String root) {
        return USER_HOME_VAR.equals(root) ? System.getProperty(USER_HOME_VAR) : root;
    }

    private static Stream<File> subDirs(File dir) {
        File[] subs = dir.listFiles(File::isDirectory); // 契约约定：I/O 失败返回 null
        return subs == null ? Stream.empty() : Arrays.stream(subs);
    }

    private boolean hasFeatureFile(File candidateDir) {
        return featureFileNames.stream().anyMatch(name -> new File(candidateDir, name).exists());
    }

    // 从进程命令行反推安装目录
    private String detectByProcess() throws Exception {
        if (processKeywords.isEmpty()) return null;

        return processLines().stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::installDirOf)
                .filter(Objects::nonNull)
                .filter(this::hasFeatureFile)
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse(null);
    }

    // 取关键字之前的命令行前缀作为候选安装目录；关键字位于行首时无父目录信息，须丢弃，否则会退化成工作目录下的相对路径误判
    private File installDirOf(String line) {
        String lowerLine = line.toLowerCase(Locale.ROOT);
        for (String keyword : processKeywords) {
            int idx = lowerLine.indexOf(keyword);
            if (idx > 0) return new File(line.substring(0, idx));
        }
        return null;
    }

    private static List<String> processLines() throws Exception {
        synchronized (PathDetector.class) { // 探测为 5 秒一次，串行进入的开销远小于一次进程扫描
            if (cachedScanLines == null || System.currentTimeMillis() - lastScanAt >= SCAN_CACHE_MILLIS) {
                cachedScanLines = runProcessScan();
                lastScanAt = System.currentTimeMillis();
            }
            return cachedScanLines;
        }
    }

    private static List<String> runProcessScan() throws Exception {
        List<String> lines = new ArrayList<>();
        Process process = new ProcessBuilder(processScanCommand()).redirectErrorStream(true).start();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(SCAN_READ_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                return; // 读完后被取消
            }
            process.destroyForcibly();
        }, "qz-process-scan-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            watchdog.interrupt();
            process.destroyForcibly(); // 读取提前结束（超时/异常）时同样要回收，否则每次扫描残留一个僵尸进程
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // 回收属关闭路径，恢复中断位即可，不覆盖主流程异常
                Thread.currentThread().interrupt();
            }
        }
        return lines;
    }

    private static String[] processScanCommand() {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            // wmic 已在新版 Windows 中被移除；-ErrorAction SilentlyContinue 屏蔽读取受保护进程 Path 的报错，防止其混入输出
            return new String[]{"powershell", "-NoProfile", "-Command",
                    "Get-Process | Select-Object -ExpandProperty Path -ErrorAction SilentlyContinue"};
        }
        // 直接 exec 而非经 sh -c：命令无 shell 语法，少依赖一层 shell，更利于裁剪过的基础镜像；-ww 避免 macOS 长命令行被截断
        return new String[]{"ps", "-ww", "-e", "-o", "args="};
    }

    // 清单值允许逗号后带空格，故逐项 trim
    private static List<String> splitCsv(String value) {
        if (value == null) return Collections.emptyList();

        List<String> tokens = new ArrayList<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) tokens.add(trimmed);
        }
        return tokens;
    }
}
