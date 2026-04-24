package qingzhou.app.demo;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Monitor;

import java.lang.management.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(code = "jvm", order = 1,
        name = {"JVM", "en:JVM"},
        info = {"JVM运行时监控", "en:JVM Runtime Monitoring"},
        icon = "Cpu",
        menu = "monitor",
        action = "monitor")
public class JvmMonitor extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.MONITORING)
    public String statsTime;

    @ModelField(
            name = {"堆内存使用(MB)", "en:Heap Used (MB)"},
            info = {"JVM堆内存已使用量", "en:JVM heap memory used"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String heapUsed;

    @ModelField(
            name = {"堆内存最大(MB)", "en:Heap Max (MB)"},
            info = {"JVM堆内存最大值", "en:JVM heap memory max"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String heapMax;

    @ModelField(
            name = {"堆内存已提交(MB)", "en:Heap Committed (MB)"},
            info = {"JVM堆内存已提交量", "en:JVM heap memory committed"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String heapCommitted;

    @ModelField(
            name = {"非堆内存使用(MB)", "en:Non-Heap Used (MB)"},
            info = {"JVM非堆内存已使用量", "en:JVM non-heap memory used"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String nonHeapUsed;

    @ModelField(
            name = {"活动线程数", "en:Live Threads"},
            info = {"当前活动线程数量", "en:Current live thread count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"线程", "en:Thread"})
    public String threadCount;

    @ModelField(
            name = {"峰值线程数", "en:Peak Threads"},
            info = {"峰值线程数量", "en:Peak thread count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"线程", "en:Thread"})
    public String peakThreadCount;

    @ModelField(
            name = {"守护线程数", "en:Daemon Threads"},
            info = {"当前守护线程数量", "en:Current daemon thread count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"线程", "en:Thread"})
    public String daemonThreadCount;

    @ModelField(
            name = {"已加载类总数", "en:Total Loaded Classes"},
            info = {"JVM已加载的类总数", "en:Total number of loaded classes"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"类加载", "en:Class Loading"})
    public String totalLoadedClassCount;

    @ModelField(
            name = {"GC总次数", "en:GC Count"},
            info = {"垃圾回收总次数", "en:Total garbage collection count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"GC", "en:GC"})
    public String gcCount;

    @ModelField(
            name = {"GC总时间(ms)", "en:GC Time (ms)"},
            info = {"垃圾回收总耗时(毫秒)", "en:Total garbage collection time in ms"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"GC", "en:GC"})
    public String gcTime;

    @ModelField(
            name = {"运行时间(秒)", "en:Uptime (s)"},
            info = {"JVM运行时长(秒)", "en:JVM uptime in seconds"},
            field_type = FieldType.MONITORING,
            numeric = true)
    public String uptime;

    @Override
    public Map<String, String> monitor(Request request) throws Exception {
        Map<String, String> data = new HashMap<>();

        data.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        data.put("heapUsed", String.valueOf(heapUsage.getUsed() / (1024 * 1024)));
        data.put("heapMax", String.valueOf(heapUsage.getMax() / (1024 * 1024)));
        data.put("heapCommitted", String.valueOf(heapUsage.getCommitted() / (1024 * 1024)));
        data.put("nonHeapUsed", String.valueOf(nonHeapUsage.getUsed() / (1024 * 1024)));

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        data.put("threadCount", String.valueOf(threadMXBean.getThreadCount()));
        data.put("peakThreadCount", String.valueOf(threadMXBean.getPeakThreadCount()));
        data.put("daemonThreadCount", String.valueOf(threadMXBean.getDaemonThreadCount()));

        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        data.put("totalLoadedClassCount", String.valueOf(classLoadingMXBean.getTotalLoadedClassCount()));

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalGcCount = 0;
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean.getCollectionCount() >= 0) {
                totalGcCount += gcBean.getCollectionCount();
            }
            if (gcBean.getCollectionTime() >= 0) {
                totalGcTime += gcBean.getCollectionTime();
            }
        }
        data.put("gcCount", String.valueOf(totalGcCount));
        data.put("gcTime", String.valueOf(totalGcTime));

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        data.put("uptime", String.valueOf(runtimeMXBean.getUptime() / 1000));

        return data;
    }
}
