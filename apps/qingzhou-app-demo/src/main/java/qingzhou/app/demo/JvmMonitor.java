package qingzhou.app.demo;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Monitor;

/**
 * JVM 运行时监控：通过 {@link ManagementFactory} 读取内存 / 线程 / 类加载 / GC 等指标。
 */
@Model(code = "jvm", order = 1,
        name = {"JVM", "en:JVM"},
        info = {"JVM运行时监控", "en:JVM Runtime Monitoring"},
        icon = "Cpu",
        menu = "monitor")
public class JvmMonitor extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.monitor)
    public String statsTime;

    @ModelField(name = {"堆内存使用(MB)", "en:Heap Used (MB)"}, info = {"JVM堆内存已使用量", "en:JVM heap memory used"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String heapUsed;

    @ModelField(name = {"堆内存最大(MB)", "en:Heap Max (MB)"}, info = {"JVM堆内存最大值", "en:JVM heap memory max"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String heapMax;

    @ModelField(name = {"堆内存已提交(MB)", "en:Heap Committed (MB)"}, info = {"JVM堆内存已提交量", "en:JVM heap memory committed"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String heapCommitted;

    @ModelField(name = {"非堆内存使用(MB)", "en:Non-Heap Used (MB)"}, info = {"JVM非堆内存已使用量", "en:JVM non-heap memory used"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String nonHeapUsed;

    @ModelField(name = {"活动线程数", "en:Live Threads"}, info = {"当前活动线程数量", "en:Current live thread count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Thread")
    public String threadCount;

    @ModelField(name = {"峰值线程数", "en:Peak Threads"}, info = {"峰值线程数量", "en:Peak thread count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Thread")
    public String peakThreadCount;

    @ModelField(name = {"守护线程数", "en:Daemon Threads"}, info = {"当前守护线程数量", "en:Current daemon thread count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "Thread")
    public String daemonThreadCount;

    @ModelField(name = {"已加载类总数", "en:Total Loaded Classes"}, info = {"JVM已加载的类总数", "en:Total number of loaded classes"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "Class")
    public String totalLoadedClassCount;

    @ModelField(name = {"GC总次数", "en:GC Count"}, info = {"垃圾回收总次数", "en:Total garbage collection count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "GC")
    public String gcCount;

    @ModelField(name = {"GC总时间(ms)", "en:GC Time (ms)"}, info = {"垃圾回收总耗时(毫秒)", "en:Total garbage collection time in ms"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "GC")
    public String gcTime;

    @ModelField(name = {"运行时间", "en:Uptime"}, info = {"JVM运行时长", "en:JVM uptime"},
            field_type = FieldType.monitor, chart_type = ChartType.stat)
    public String uptime;

    private static final int MB = 1024 * 1024;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> d = new HashMap<>();
        d.put("statsTime", DateKit.now());

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        d.put("heapUsed", String.valueOf(heap.getUsed() / MB));
        d.put("heapMax", String.valueOf(heap.getMax() / MB));
        d.put("heapCommitted", String.valueOf(heap.getCommitted() / MB));
        d.put("nonHeapUsed", String.valueOf(nonHeap.getUsed() / MB));

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        d.put("threadCount", String.valueOf(threads.getThreadCount()));
        d.put("peakThreadCount", String.valueOf(threads.getPeakThreadCount()));
        d.put("daemonThreadCount", String.valueOf(threads.getDaemonThreadCount()));

        d.put("totalLoadedClassCount", String.valueOf(ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount()));

        long totalGcCount = 0, totalGcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() >= 0) totalGcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() >= 0) totalGcTime += gc.getCollectionTime();
        }
        d.put("gcCount", String.valueOf(totalGcCount));
        d.put("gcTime", String.valueOf(totalGcTime));

        d.put("uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
        return d;
    }

    /** 把毫秒运行时间格式化为 {@code NdHhMmSs}。 */
    private static String formatUptime(long ms) {
        long s = ms / 1000;
        long days = s / 86400;
        long hours = (s % 86400) / 3600;
        long minutes = (s % 3600) / 60;
        long secs = s % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0 || days > 0) sb.append(hours).append("h");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m");
        sb.append(secs).append("s");
        return sb.toString();
    }
}
