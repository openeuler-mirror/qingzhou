package qingzhou.monitor.collector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import qingzhou.monitor.api.Metric;
import qingzhou.monitor.api.MetricProvider;

@Component(property = MetricProvider.PROVIDER_ID + "=jvm")
public class JvmMetricProvider implements MetricProvider {
    private static final Metric.Type GAUGE = Metric.Type.GAUGE;
    private static final Metric.Type COUNTER = Metric.Type.COUNTER;

    @Override
    public Set<Metric> collectMetrics() {
        Set<Metric> metrics = new HashSet<>();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        metrics.add(Metric.of("memory_heap_used_bytes", heap.getUsed(), GAUGE,
                "Heap memory currently used in bytes"));
        // max 未定义时为 -1，属缺失值而非真实用量，不输出样本
        if (heap.getMax() >= 0) {
            metrics.add(Metric.of("memory_heap_max_bytes", heap.getMax(), GAUGE,
                    "Maximum heap memory in bytes, absent when undefined"));
        }
        metrics.add(Metric.of("memory_heap_committed_bytes", heap.getCommitted(), GAUGE,
                "Heap memory committed to the JVM in bytes"));
        metrics.add(Metric.of("memory_non_heap_used_bytes", nonHeap.getUsed(), GAUGE,
                "Non-heap memory currently used in bytes"));

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        metrics.add(Metric.of("threads_live", threads.getThreadCount(), GAUGE, "Live thread count"));
        metrics.add(Metric.of("threads_daemon", threads.getDaemonThreadCount(), GAUGE, "Live daemon thread count"));
        metrics.add(Metric.of("threads_peak", threads.getPeakThreadCount(), GAUGE, "Peak live thread count since JVM start"));
        metrics.add(Metric.of("classes_loaded_total",
                ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(), COUNTER,
                "Total number of classes loaded since JVM start"));

        long gcCount = 0;
        long gcTime = 0;
        // MXBean 约定 -1 表示统计不可用，按 0 计入总量
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(gc.getCollectionCount(), 0);
            gcTime += Math.max(gc.getCollectionTime(), 0);
        }
        metrics.add(Metric.of("gc_collection_total", gcCount, COUNTER,
                "Total garbage collection count across all collectors"));
        metrics.add(Metric.of("gc_time_seconds_total", gcTime / 1000d, COUNTER,
                "Total garbage collection time in seconds across all collectors"));
        metrics.add(Metric.of("uptime_seconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, GAUGE,
                "JVM uptime in seconds"));
        return metrics;
    }
}
