package qingzhou.monitor.prometheus;

import java.util.*;

import qingzhou.monitor.api.Metric;

final class MetricFormat {
    private static final String PREFIX = "qingzhou_";

    static final String PROVIDER = "provider";

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
                        .append(line(name, metric, id)).append('\n');
            }
        }
        StringBuilder out = new StringBuilder();
        types.forEach((name, type) -> out.append("# TYPE ").append(name).append(' ')
                .append(type.name().toLowerCase(Locale.ROOT)).append('\n').append(bodies.get(name)));
        return out.toString();
    }

    private static String canonicalName(String raw) {
        return raw.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static String escapeLabelValue(String value) {
        // \r 改空格：Prometheus 只接受 \\、\"、\n 三种转义，直接输出 \r 会让样本解析失败
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", " ");
    }

    private static String line(String name, Metric metric, String id) {
        Map<String, String> labels = new LinkedHashMap<>(metric.labels());
        labels.put(PROVIDER, id);
        StringJoiner out = new StringJoiner(",", name + "{", "}");
        labels.forEach((key, value) -> out.add(key + "=\"" + escapeLabelValue(value) + "\""));
        return out.toString() + " " + metric.value();
    }
}
