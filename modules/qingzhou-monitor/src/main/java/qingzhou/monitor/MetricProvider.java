package qingzhou.monitor;

import java.util.List;

/**
 * 监控指标提供方：其他模块以 DS 组件实现本接口即自动注册（{@code @Component}），
 * scrape /prometheus 时被同步调用采集系统状态指标。
 */
public interface MetricProvider {

    /**
     * 提供方标识，将作为输出指标的 provider 标签。
     */
    String getName();

    /**
     * 采集一批指标。指标名无需带 qingzhou_ 前缀，端点会自动补齐。
     */
    List<Metric> collect() throws Exception;
}
