package qingzhou.engine.detector;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 探测 = 多来源候选目录 → 特征文件判定。环境变量、扫描根（自身与一级子目录）、进程命令行反推三路候选合并去重后统一过滤：
// 来源只决定"候选目录从哪来"，任一来源命中即计入，不再按来源短路——配置目录与运行态目录可能是不同实例，都要发现
class PathDetector {
    private static final String USER_HOME_VAR = "user.home";

    private final List<String> featureFileNames;
    private final List<String> envVarNames;
    private final List<String> scanRootPaths;
    private final ProcessParser processParser;

    PathDetector(Dictionary<String, String> attributes) {
        // 清单值在构造期一次解析，避免每拍探测重复 split
        featureFileNames = splitCsv(attributes.get("Qingzhou-Detection-Feature-Files"));

        envVarNames = splitCsv(attributes.get("Qingzhou-Detection-Env-Vars"));

        List<String> roots = splitCsv(attributes.get("Qingzhou-Detection-Scan-Roots"));
        // 清单未给出时回落到 JVM 属性，便于部署期覆盖扫描范围
        scanRootPaths = roots.isEmpty() ? splitCsv(System.getProperty("Qingzhou-Detection-Scan-Roots")) : roots;

        List<String> processKeywords = splitCsv(attributes.get("Qingzhou-Detection-Process-Names")).stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        String processHomeRegex = attributes.get("Qingzhou-Detection-Process-Home-Regex");
        // 从进程命令行提取安装目录的正则（须含捕获组），由清单 Qingzhou-Detection-Process-Home-Regex 配置；未配置时退回关键字前缀
        Pattern pattern = processHomeRegex == null || processHomeRegex.isEmpty() ? null : Pattern.compile(processHomeRegex);
        processParser = new ProcessParser(processKeywords, pattern);
    }

    // 结果无序：listFiles 与 ps 输出顺序均未定义，调用方不得依赖位次
    Set<String> detect() {
        List<File> candidates = new ArrayList<>();
        candidates.addAll(envCandidateDirs());
        candidates.addAll(rootCandidateDirs());
        candidates.addAll(processParser.processCandidateDirs());
        return candidates.stream()
                .filter(this::hasFeatureFile)
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());
    }

    // 唯一的"是不是目标安装目录"判据：三路来源共用
    private boolean hasFeatureFile(File dir) {
        return featureFileNames.stream().anyMatch(name -> new File(dir, name).exists());
    }

    private List<File> envCandidateDirs() {
        return envVarNames.stream()
                .map(System::getenv)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(File::new)
                .collect(Collectors.toList());
    }

    private List<File> rootCandidateDirs() {
        List<File> dirs = new ArrayList<>();
        for (String root : scanRootPaths) {
            File dir = new File(USER_HOME_VAR.equals(root) ? System.getProperty(USER_HOME_VAR) : root);
            if (!dir.isDirectory()) continue;
            dirs.add(dir);
            File[] subs = dir.listFiles(File::isDirectory); // 契约约定：I/O 失败返回 null
            if (subs != null) dirs.addAll(Arrays.asList(subs));
        }
        return dirs;
    }

    // 清单值允许逗号后带空格，故逐项 trim
    private static List<String> splitCsv(String value) {
        if (value == null) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }
}
