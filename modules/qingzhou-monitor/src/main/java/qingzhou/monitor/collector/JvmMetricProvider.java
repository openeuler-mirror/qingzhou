package qingzhou.monitor.collector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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

        // 按回收器分列：Young 与 Full 的耗时量级不同，合并成总量无法定界；label 值取值域由 JVM 实现固定，基数受控
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, String> gcLabels = Collections.singletonMap("gc", gc.getName());
            // MXBean 约定 -1 表示该项统计不可用，按缺失处理不输出样本
            long count = gc.getCollectionCount();
            if (count >= 0) {
                metrics.add(Metric.of("gc_collection_total", count, COUNTER, gcLabels,
                        "Total garbage collection count of the collector"));
            }
            long time = gc.getCollectionTime();
            if (time >= 0) {
                metrics.add(Metric.of("gc_time_seconds_total", time / 1000d, COUNTER, gcLabels,
                        "Total garbage collection time in seconds of the collector"));
            }
        }
        metrics.add(Metric.of("uptime_seconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, GAUGE,
                "JVM uptime in seconds"));
        return metrics;
    }
}
