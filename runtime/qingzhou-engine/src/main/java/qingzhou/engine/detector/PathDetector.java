package qingzhou.engine.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

// 协调器：构造期一次性解析清单，按序尝试各策略，返回首个命中；清单值在构造期一次解析，避免每拍探测重复 split
class PathDetector {
    private final List<PathDetectionStrategy> strategies;

    PathDetector(Dictionary<String, String> attributes) {
        FeatureFiles featureFiles = new FeatureFiles(splitCsv(attributes.get("Qingzhou-Detection-Feature-Files")));

        List<String> roots = splitCsv(attributes.get("Qingzhou-Detection-Scan-Roots"));
        // 清单未给出时回落到 JVM 属性，便于部署期覆盖扫描范围
        if (roots.isEmpty()) roots = splitCsv(System.getProperty("Qingzhou-Detection-Scan-Roots"));

        List<PathDetectionStrategy> list = new ArrayList<>();
        list.add(new FeatureFileStrategy(featureFiles,
                splitCsv(attributes.get("Qingzhou-Detection-Env-Vars")), roots));
        list.add(new ProcessStrategy(featureFiles,
                splitCsv(attributes.get("Qingzhou-Detection-Process-Names")),
                attributes.get("Qingzhou-Detection-Process-Home-Regex")));
        this.strategies = list;
    }

    // 协调器：按序尝试各策略，返回首个非空结果；全部落空返回空列表（不用 null 表达，免去调用方判空）
    List<String> detect() throws Exception {
        for (PathDetectionStrategy strategy : strategies) {
            List<String> detectedList = strategy.detect();
            if (!detectedList.isEmpty()) return detectedList;
        }
        return Collections.emptyList();
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
