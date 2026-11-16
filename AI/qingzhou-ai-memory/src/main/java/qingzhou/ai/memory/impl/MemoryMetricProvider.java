package qingzhou.ai.memory.impl;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

/**
 * 记忆占用：读派生索引的只读快照，抓取不进入会话读写的同步块。
 */
@Component(property = MetricProvider.PROVIDER_ID + "=ai_memory")
public class MemoryMetricProvider implements MetricProvider {
    @Reference
    private ConversationStoreImpl store;

    @Override
    public Set<Metric> collectMetrics() {
        ConversationCatalog catalog = store.catalog();
        Set<Metric> metrics = new HashSet<>();
        metrics.add(Metric.of("conversations_active", catalog.activeCount(), Metric.Type.GAUGE,
                "Active conversations tracked by the index"));
        metrics.add(Metric.of("conversations_archived", catalog.archivedCount(), Metric.Type.GAUGE,
                "Archived conversations tracked by the index"));
        metrics.add(Metric.of("store_bytes", catalog.totalBytes(), Metric.Type.GAUGE,
                "Total bytes of the conversation records"));
        return metrics;
    }
}
