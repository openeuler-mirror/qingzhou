package qingzhou.monitor.api;

import java.util.Map;
import java.util.Set;

/**
 * 按 provider 分组的指标集合，跨实例报文载体。
 * 外层具体类是必需：Json 只有 Class<T> 重载，泛型容器靠字段签名还原 Metric 元素类型。
 */
public final class Metrics {
    private Map<String, Set<Metric>> metricsByProvider;

    // 契约适配：Json 按 JavaBean 约定反序列化
    public Metrics() {
    }

    public static Metrics of(Map<String, Set<Metric>> metricsByProvider) {
        Metrics metrics = new Metrics();
        metrics.metricsByProvider = metricsByProvider;
        return metrics;
    }

    public Map<String, Set<Metric>> getMetricsByProvider() {
        return metricsByProvider;
    }

    public void setMetricsByProvider(Map<String, Set<Metric>> metricsByProvider) {
        this.metricsByProvider = metricsByProvider;
    }
}
