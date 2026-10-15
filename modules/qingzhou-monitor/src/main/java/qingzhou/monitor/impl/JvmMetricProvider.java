package qingzhou.monitor.impl;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import qingzhou.monitor.Metric;
import qingzhou.monitor.MetricProvider;

/**
 * 内置进程级 JVM 指标：标准 ManagementFactory 采集内存/线程/类加载/GC/运行时长。
 */
@Component
public class JvmMetricProvider implements MetricProvider {
    @Override
    public String getName() {
        return "jvm";
    }

    @Override
    public List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        metrics.add(new Metric("process_memory_heap_used_bytes", heap.getUsed()));
        metrics.add(new Metric("process_memory_heap_max_bytes", heap.getMax()));
        metrics.add(new Metric("process_memory_heap_committed_bytes", heap.getCommitted()));
        metrics.add(new Metric("process_memory_non_heap_used_bytes", nonHeap.getUsed()));

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        metrics.add(new Metric("process_threads_live", threads.getThreadCount()));
        metrics.add(new Metric("process_threads_daemon", threads.getDaemonThreadCount()));
        metrics.add(new Metric("process_threads_peak", threads.getPeakThreadCount()));
        metrics.add(new Metric("process_classes_loaded_total",
                ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount()));

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(gc.getCollectionCount(), 0);
            gcTime += Math.max(gc.getCollectionTime(), 0);
        }
        metrics.add(new Metric("process_gc_collection_total", gcCount));
        metrics.add(new Metric("process_gc_time_milliseconds_total", gcTime));
        metrics.add(new Metric("process_uptime_seconds",
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000d));
        return metrics;
    }
}
