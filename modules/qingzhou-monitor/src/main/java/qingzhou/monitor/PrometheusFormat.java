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
     * 同名指标类型或说明冲突、同一序列重复出现，均属编程错误，直接抛出。
     */
    static String render(Map<String, Collection<Metric>> metricsByProvider) {
        Map<String, Metric.Type> types = new LinkedHashMap<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        Map<String, StringBuilder> bodies = new LinkedHashMap<>();
        Set<String> series = new HashSet<>();
        for (Map.Entry<String, Collection<Metric>> entry : metricsByProvider.entrySet()) {
            String id = entry.getKey();
            for (Metric metric : entry.getValue()) {
                String name = PREFIX + canonicalName(id + "_" + metric.name());
                Metric.Type registeredType = types.putIfAbsent(name, metric.type());
                if (registeredType != null && registeredType != metric.type()) {
                    throw new IllegalStateException("conflicting metric type for " + name);
                }
                String description = metric.description();
                String registeredDescription = descriptions.putIfAbsent(name, description);
                if (registeredDescription != null && !registeredDescription.equals(description)) {
                    throw new IllegalStateException("conflicting metric description for " + name);
                }
                // 同族重复序列会被抓取端判为 duplicate sample 并丢弃整批，须在渲染边界拦下；族名已含 provider 前缀，跨 provider 不会误判
                Map<String, String> labels = labelsOf(id, metric);
                if (!series.add(name + labels)) {
                    throw new IllegalStateException("duplicate metric series: " + name + labels);
                }
                bodies.computeIfAbsent(name, key -> new StringBuilder())
                        .append(line(name, labels, metric.value())).append('\n');
            }
        }
        StringBuilder out = new StringBuilder();
        types.forEach((name, type) -> {
            // 空说明表示该指标族未提供文档，省略 HELP；HELP 须先于 TYPE 输出
            String description = descriptions.get(name);
            if (!description.isEmpty()) {
                out.append("# HELP ").append(name).append(' ').append(escapeHelp(description)).append('\n');
            }
            out.append("# TYPE ").append(name).append(' ').append(type.name().toLowerCase(Locale.ROOT))
                    .append('\n').append(bodies.get(name));
        });
        return out.toString();
    }

    // 指标名须匹配 Prometheus [a-zA-Z_:][a-zA-Z0-9_:]*：拆驼峰、统一小写、其余非法字符（含保留给 recording rule 的冒号）改写为下划线。
    // 名称由内部 id/name 拼接，改写不会引入外部键冲突，故不像 labelName 那样抛错；首字符合法性由调用方的 qingzhou_ 前缀保证。
    private static String canonicalName(String raw) {
        return raw.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    // 用 TreeMap：标签是集合，判重与输出均不应受插入顺序影响
    private static Map<String, String> labelsOf(String id, Metric metric) {
        Map<String, String> labels = new TreeMap<>(metric.labels());
        labels.put(PROVIDER, id);
        return labels;
    }

    private static String line(String name, Map<String, String> labels, double value) {
        StringJoiner out = new StringJoiner(",", name + "{", "}");
        labels.forEach((key, labelValue) -> out.add(labelName(key) + "=\"" + escapeLabelValue(labelValue) + "\""));
        return out + " " + value;
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

    private static String escapeHelp(String description) {
        // HELP 文本只接受 \\ 与 \n 两种转义，引号无需转义；\r 改空格以免行内回车破坏解析
        return description.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", " ");
    }
}
