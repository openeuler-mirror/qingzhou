package qingzhou.monitor.prometheus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import qingzhou.monitor.Metric;

/**
 * 抓取期样本汇总：命名规范化、同名类型归一、重复序列剔除、自监控命名空间保护。
 * 违反项只丢弃该样本并计数，不影响整次抓取，避免单个应用或提供方把端点打成 500。
 */
final class MetricBuffer {
    private final Map<String, Metric.Type> types = new HashMap<>();
    private final Map<String, Sample> series = new LinkedHashMap<>(); // 保序，输出稳定

    void add(Sample sample, DropCounter drops) {
        String name = MetricFormat.canonicalName(sample.name);
        if (MetricFormat.SELF_NAMESPACE.equals(name) || name.startsWith(MetricFormat.SELF_NAMESPACE + "_")) {
            drops.add(DropCounter.Reason.RESERVED_NAME, "metric=" + name);
            return;
        }
        Metric.Type type = types.get(name);
        if (type == null) {
            types.put(name, sample.type);
        } else if (type != sample.type) {
            drops.add(DropCounter.Reason.TYPE_CONFLICT, "metric=" + name);
            return;
        }
        if (series.putIfAbsent(seriesKey(name, sample.labels), sample) != null) {
            drops.add(DropCounter.Reason.DUPLICATE_SERIES, "metric=" + name);
        }
    }

    List<Sample> metrics() {
        return new ArrayList<>(series.values());
    }

    // 标签顺序不影响序列身份，须排序后再拼 key
    private static String seriesKey(String name, Map<String, String> labels) {
        return name + new TreeMap<>(labels);
    }
}
