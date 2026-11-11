package qingzhou.monitor.prometheus;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.monitor.Metric;

/** 端点侧样本模型：提供方样本在此完成改名与内置标签合并。 */
public final class Sample {
    final String name;
    final double value;
    final Metric.Type type;
    final Map<String, String> labels;

    public Sample(String name, double value, Metric.Type type, Map<String, String> labels) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.labels = labels;
    }

    static Sample of(Metric metric, String name, Map<String, String> labels) {
        Map<String, String> merged = new LinkedHashMap<>(metric.labels());
        merged.putAll(labels);
        return new Sample(name, metric.value(), metric.type(), merged);
    }
}
