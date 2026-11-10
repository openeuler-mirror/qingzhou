package qingzhou.monitor.prometheus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import qingzhou.monitor.Metric;

/**
 * Prometheus 0.0.4 文本格式：指标名规范化与 exposition 渲染。
 * 命名空间约定：应用字段为 qingzhou_&lt;字段&gt;，提供方为 qingzhou_&lt;providerId&gt;_&lt;指标&gt;，
 * 端点自监控独占 qingzhou_monitor_ 前缀（应用字段与提供方 id 不得占用，见 {@link #SELF_NAMESPACE}）。
 */
final class MetricFormat {
    private static final String PREFIX = "qingzhou_";

    /** 端点自监控命名空间：应用字段与提供方 id 占用会导致自监控序列被同名样本污染 */
    static final String SELF_NAMESPACE = "monitor";

    /** 抓取端注入标签与端点内置标签：扩展方 labels 不得占用，否则内置标签被覆盖 */
    static final Set<String> RESERVED_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "instance", "job", "qingzhou_instance", "app", "model", "field", "provider")));

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

    /**
     * 渲染 exposition。调用方须保证同名指标类型一致（抓取期由 MetricBuffer 归一），
     * 违反属编程错误，直接抛出。
     */
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
