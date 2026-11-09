package qingzhou.app.tomcat;

import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.action.Monitor;

@Model(code = "jvm",
        order = 1,
        name = {"JVM监控", "en:JVM Monitor"},
        info = {"运行中 Tomcat 进程的 JVM 指标", "en:JVM metrics of the running Tomcat process"},
        icon = "Cpu")
public class TomcatJvm extends TomcatModelBase implements Monitor {

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"运行时长(小时)", "en:Uptime (h)"},
            info = {"JVM 已运行时长", "en:JVM uptime in hours"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String uptimeHours;

    @ModelField(
            name = {"进程CPU(%)", "en:Process CPU (%)"},
            info = {"Tomcat 进程的 CPU 使用率，-1 表示当前平台不可用", "en:CPU usage of the Tomcat process"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "Basic")
    public String processCpuPercent;

    @ModelField(
            name = {"JVM线程数", "en:JVM Threads"},
            info = {"JVM 当前线程总数", "en:Current JVM thread count"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Basic")
    public String jvmThreadCount;

    @ModelField(
            name = {"已加载类", "en:Loaded Classes"},
            info = {"JVM 已加载的类数量", "en:Loaded classes"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String loadedClassCount;

    @ModelField(
            name = {"堆已用(MB)", "en:Heap Used (MB)"},
            info = {"JVM 堆内存已使用量", "en:Heap memory used"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Memory")
    public String heapUsedMb;

    @ModelField(
            name = {"堆上限(MB)", "en:Heap Max (MB)"},
            info = {"JVM 堆内存上限", "en:Heap memory maximum"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Memory")
    public String heapMaxMb;

    @ModelField(
            name = {"堆使用率(%)", "en:Heap Used (%)"},
            info = {"堆内存使用量占上限的比例", "en:Heap usage ratio"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "Memory")
    public String heapUsedPercent;

    @ModelField(
            name = {"非堆已用(MB)", "en:Non-Heap Used (MB)"},
            info = {"JVM 非堆内存已使用量", "en:Non-heap memory used"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Memory")
    public String nonHeapUsedMb;

    @ModelField(
            name = {"GC次数", "en:GC Count"},
            info = {"各垃圾回收器的累计回收次数", "en:Total garbage collection count"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "GC")
    public String gcCount;

    @ModelField(
            name = {"GC耗时(ms)", "en:GC Time (ms)"},
            info = {"各垃圾回收器的累计回收耗时", "en:Total garbage collection time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "GC")
    public String gcTimeMs;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        return TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::jvmStats);
    }
}
