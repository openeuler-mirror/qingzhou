package qingzhou.monitor.prometheus;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.monitor.Metric;

/**
 * 端点侧样本模型：提供方样本在入缓冲时一次性完成改名与内置标签合并，端点内部只消费本类型，
 * 后续增删样本字段不影响指标提供方。
 */
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

    /** 提供方样本入缓冲：name 含提供方命名空间，labels 为端点内置标签 */
    static Sample of(Metric metric, String name, Map<String, String> labels) {
        Map<String, String> merged = new LinkedHashMap<>(metric.labels());
        merged.putAll(labels);
        return new Sample(name, metric.value(), metric.type(), merged);
    }
}
