package qingzhou.monitor;

import java.util.List;

/**
 * 指标提供方 SPI：端点按需拉取当前样本快照，指标名前缀与内置标签由端点统一补齐。
 */
public interface MetricProvider {

    /**
     * 组件属性键：提供方标识，用作指标命名空间前缀与 provider 标签值。
     */
    String PROVIDER_ID = "METRIC_PROVIDER_ID";

    /**
     * @return 当前指标快照，不得为 null（无指标时返回空集合）；由端点单一抓取线程串行调用
     */
    List<Metric> collectMetrics();
}
