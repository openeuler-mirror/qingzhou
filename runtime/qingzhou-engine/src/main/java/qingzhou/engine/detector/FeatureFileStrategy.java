package qingzhou.engine.detector;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// 文件策略：候选目录按优先级为环境变量指向的目录、扫描根目录自身及其一级子目录；流惰性求值，首个命中即短路
class FeatureFileStrategy implements PathDetectionStrategy {
    private static final String USER_HOME_VAR = "user.home";

    private final FeatureFiles featureFiles;
    private final List<String> envVarNames;
    private final List<String> scanRootPaths;

    FeatureFileStrategy(FeatureFiles featureFiles, List<String> envVarNames, List<String> scanRootPaths) {
        this.featureFiles = featureFiles;
        this.envVarNames = envVarNames;
        this.scanRootPaths = scanRootPaths;
    }

    @Override
    public String detect() {
        Stream<File> envCandidates = envVarNames.stream()
                .map(System::getenv)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(File::new);
        return Stream.concat(envCandidates, rootCandidates())
                .filter(featureFiles::containsIn)
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse(null);
    }

    private Stream<File> rootCandidates() {
        return scanRootPaths.stream()
                .map(FeatureFileStrategy::resolveHomeVar)
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
}
