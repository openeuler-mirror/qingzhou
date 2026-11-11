package qingzhou.prometheus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import qingzhou.monitor.Metric;

/** Prometheus 0.0.4 文本格式：指标名规范化与 exposition 渲染。 */
final class MetricFormat {
    private static final String PREFIX = "qingzhou_";

    private MetricFormat() {
    }

    /** 指标名规范化：camelCase 转下划线、小写、非法字符替换；不含前缀 */
    static String canonicalName(String raw) {
        if (raw == null) throw new IllegalArgumentException("empty metric name");
        String name = raw.trim().replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (name.isEmpty() || "_".equals(name)) throw new IllegalArgumentException("empty metric name: " + raw);
        return name;
    }

    /** 渲染 exposition；同名指标类型冲突属编程错误，直接抛出。 */
    static String render(Collection<Sample> metrics) {
        Map<String, Group> groups = new LinkedHashMap<>();
        for (Sample metric : metrics) {
            String name = PREFIX + canonicalName(metric.name);
            Group group = groups.get(name);
            if (group == null) {
                group = new Group(name, metric.type);
                groups.put(name, group);
            } else if (group.type != metric.type) {
                throw new IllegalStateException("conflicting metric type for " + name);
            }
            group.lines.add(line(name, metric));
        }

        StringBuilder out = new StringBuilder();
        for (Group group : groups.values()) {
            out.append("# TYPE ").append(group.name).append(' ')
                    .append(group.type.name().toLowerCase(Locale.ROOT)).append('\n');
            for (String text : group.lines) out.append(text).append('\n');
        }
        return out.toString();
    }

    private static String escapeLabelValue(String value) {
        // \r 改空格：Prometheus 只接受 \\、\"、\n 三种转义，直接输出 \r 会让样本解析失败
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", " ");
    }

    private static String line(String name, Sample sample) {
        StringBuilder result = new StringBuilder(name).append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : sample.labels.entrySet()) {
            if (!first) result.append(',');
            result.append(entry.getKey()).append("=\"").append(escapeLabelValue(entry.getValue())).append('"');
            first = false;
        }
        return result.append("} ").append(sample.value).toString();
    }

    private static final class Group {
        final String name;
        final Metric.Type type;
        final List<String> lines = new ArrayList<>();

        Group(String name, Metric.Type type) {
            this.name = name;
            this.type = type;
        }
    }
}
