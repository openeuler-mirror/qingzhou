package qingzhou.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class PathDetector {
    // ================= 核心探测逻辑 =================
    static String detect(String detectionFeatureFiles, String detectionEnvVars, String scanRoots) {
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
                .filter(p -> Arrays.stream(detectionFeatureFiles.split(",")).anyMatch(f -> !f.isEmpty() && Files.exists(Paths.get(p, f))))
                .findFirst()
                .orElse(null);
    }
}
