package qingzhou.registry.impl;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;
import qingzhou.registry.Registry;

/**
 * 注册表规模：集群在线节点与应用数量的直接表征，取值为既有只读查询，不触碰注册表锁。
 */
@Component(property = MetricProvider.PROVIDER_ID + "=registry")
public class RegistryMetricProvider implements MetricProvider {
    @Reference
    private Registry registry;

    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("remote_instances", registry.getAllRemoteInstances().size(), Metric.Type.GAUGE,
                "Remote instances currently registered"));
        metrics.add(Metric.of("local_apps", registry.getAllLocalApps().size(), Metric.Type.GAUGE,
                "Applications registered in this instance"));
        return metrics;
    }
}
