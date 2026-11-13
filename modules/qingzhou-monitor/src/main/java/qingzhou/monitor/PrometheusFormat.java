package qingzhou.monitor;

import java.util.*;
import java.util.regex.Pattern;

import qingzhou.monitor.api.Metric;

final class PrometheusFormat {
    static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";
    static final String PROVIDER = "provider";
    private static final String PREFIX = "qingzhou_";
    private static final Pattern LABEL_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * 同名指标类型冲突属编程错误，直接抛出。
     */
    static String render(Map<String, Collection<Metric>> metricsByProvider) {
        Map<String, Metric.Type> types = new LinkedHashMap<>();
        Map<String, StringBuilder> bodies = new LinkedHashMap<>();
        for (Map.Entry<String, Collection<Metric>> entry : metricsByProvider.entrySet()) {
            String id = entry.getKey();
            for (Metric metric : entry.getValue()) {
                String name = PREFIX + canonicalName(id + "_" + metric.name());
                Metric.Type registered = types.putIfAbsent(name, metric.type());
                if (registered != null && registered != metric.type()) {
                    throw new IllegalStateException("conflicting metric type for " + name);
                }
                bodies.computeIfAbsent(name, key -> new StringBuilder())
                        .append(line(id, name, metric)).append('\n');
            }
        }
        StringBuilder out = new StringBuilder();
        types.forEach((name, type) -> out.append("# TYPE ").append(name).append(' ')
                .append(type.name().toLowerCase(Locale.ROOT)).append('\n').append(bodies.get(name)));
        return out.toString();
    }

    // 指标名须匹配 Prometheus [a-zA-Z_:][a-zA-Z0-9_:]*：拆驼峰、统一小写、其余非法字符（含保留给 recording rule 的冒号）改写为下划线。
    // 名称由内部 id/name 拼接，改写不会引入外部键冲突，故不像 labelName 那样抛错；首字符合法性由调用方的 qingzhou_ 前缀保证。
    private static String canonicalName(String raw) {
        return raw.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static String line(String id, String name, Metric metric) {
        Map<String, String> labels = new LinkedHashMap<>(metric.labels());
        labels.put(PROVIDER, id);
        StringJoiner out = new StringJoiner(",", name + "{", "}");
        labels.forEach((key, value) -> out.add(labelName(key) + "=\"" + escapeLabelValue(value) + "\""));
        return out + " " + metric.value();
    }

    // 非法 label 名会污染整段 exposition，在渲染边界一次性校验并抛错；不做静默改写以免引发键冲突
    private static String labelName(String key) {
        if (!LABEL_NAME.matcher(key).matches() || key.startsWith("__"))
            throw new IllegalArgumentException("illegal label name: " + key);
        return key;
    }

    private static String escapeLabelValue(String value) {
        // \r 改空格：Prometheus 只接受 \\、\"、\n 三种转义，直接输出 \r 会让样本解析失败
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", " ");
    }
}
