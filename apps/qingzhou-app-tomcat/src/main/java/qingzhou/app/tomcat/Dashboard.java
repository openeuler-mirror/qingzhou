package qingzhou.app.tomcat;

import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Show;

@Model(code = "dashboard",
        order = 0,
        name = {"首页", "en:Dashboard"},
        info = {"Tomcat 运行监控", "en:Tomcat runtime monitoring"},
        icon = "HomeFilled")
public class Dashboard extends TomcatModelBase implements Show, Monitor {

    @ModelField(id = true, show = false,
            name = {"编号", "en:ID"},
            field_type = FieldType.monitor)
    public String id;

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"监控数据采集时间", "en:Statistics generation time"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String statsTime;

    @ModelField(
            name = {"安装路径", "en:Install Path"},
            info = {"Tomcat 安装路径", "en:Tomcat installation path"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String path;

    @ModelField(
            name = {"服务器版本", "en:Server Info"},
            info = {"Tomcat 版本信息", "en:Tomcat version"},
            field_type = FieldType.monitor,
            group = "Basic")
    public String serverInfo;

    @ModelField(
            name = {"通道数", "en:Connectors"},
            info = {"当前 Connector 数量", "en:Number of connectors"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String connectorCount;

    @ModelField(
            name = {"应用数", "en:Applications"},
            info = {"当前部署的应用数量", "en:Number of deployed applications"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Basic")
    public String applicationCount;

    @ModelField(
            name = {"活跃会话数", "en:Active Sessions"},
            info = {"各应用活跃会话总数", "en:Total active sessions"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Session")
    public String activeSessions;

    @ModelField(
            name = {"累计请求数", "en:Requests"},
            info = {"自启动以来的累计请求数", "en:Requests since startup"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Request")
    public String requestCount;

    @ModelField(
            name = {"累计错误数", "en:Errors"},
            info = {"自启动以来的累计错误数", "en:Errors since startup"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Request")
    public String errorCount;

    @ModelField(
            name = {"已发送(MB)", "en:Sent (MB)"},
            info = {"累计发送字节数", "en:Bytes sent"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Traffic")
    public String bytesSent;

    @ModelField(
            name = {"已接收(MB)", "en:Received (MB)"},
            info = {"累计接收字节数", "en:Bytes received"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Traffic")
    public String bytesReceived;

    @ModelField(
            name = {"线程繁忙率(%)", "en:Busy Threads (%)"},
            info = {"繁忙线程数占最大线程数的比例", "en:Busy threads ratio"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "Thread")
    public String threadBusyPercent;

    @ModelField(
            name = {"繁忙线程", "en:Busy Threads"},
            info = {"当前处理请求的线程数", "en:Threads processing requests"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "Thread")
    public String threadBusy;

    @ModelField(
            name = {"空闲线程", "en:Idle Threads"},
            info = {"当前可用的空闲线程数", "en:Idle threads"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.bar,
            group = "Thread")
    public String threadIdle;

    @ModelField(
            name = {"平均处理时间(ms)", "en:Avg Time (ms)"},
            info = {"请求平均处理耗时", "en:Average processing time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Latency")
    public String avgProcessingTime;

    @ModelField(
            name = {"最大处理时间(ms)", "en:Max Time (ms)"},
            info = {"请求历史最大处理耗时", "en:Maximum processing time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.line,
            group = "Latency")
    public String maxProcessingTime;

    @ModelField(
            name = {"进程CPU(%)", "en:Process CPU (%)"},
            info = {"Tomcat 进程的 CPU 使用率，-1 表示当前平台不可用", "en:CPU usage of the Tomcat process"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "Jvm")
    public String processCpuPercent;

    @ModelField(
            name = {"系统CPU(%)", "en:System CPU (%)"},
            info = {"整机 CPU 使用率", "en:System-wide CPU usage"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String systemCpuPercent;

    @ModelField(
            name = {"JVM线程数", "en:JVM Threads"},
            info = {"JVM 当前线程总数", "en:Current JVM thread count"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String jvmThreadCount;

    @ModelField(
            name = {"已加载类", "en:Loaded Classes"},
            info = {"JVM 已加载的类数量", "en:Loaded classes"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String loadedClassCount;

    @ModelField(
            name = {"GC次数", "en:GC Count"},
            info = {"各垃圾回收器的累计回收次数", "en:Total garbage collection count"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String gcCount;

    @ModelField(
            name = {"GC耗时(ms)", "en:GC Time (ms)"},
            info = {"各垃圾回收器的累计回收耗时", "en:Total garbage collection time"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String gcTimeMs;

    @ModelField(
            name = {"运行时长(小时)", "en:Uptime (h)"},
            info = {"JVM 已运行时长", "en:JVM uptime in hours"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Jvm")
    public String uptimeHours;

    @ModelField(
            name = {"堆已用(MB)", "en:Heap Used (MB)"},
            info = {"JVM 堆内存已使用量", "en:Heap memory used"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Memory")
    public String heapUsedMb;

    @ModelField(
            name = {"堆上限(MB)", "en:Heap Max (MB)"},
            info = {"JVM 堆内存上限", "en:Heap memory maximum"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
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
            chart_type = ChartType.stat,
            group = "Memory")
    public String nonHeapUsedMb;

    @ModelField(
            name = {"物理内存可用(MB)", "en:Free Physical (MB)"},
            info = {"操作系统可用物理内存", "en:Free physical memory"},
            field_type = FieldType.monitor,
            numeric = true,
            chart_type = ChartType.stat,
            group = "Memory")
    public String physicalFreeMb;

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> stats;
        try {
            stats = TomcatJmx.read(getTomcatBaseDir(), TomcatJmx::stats);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read Tomcat metrics via JMX", e);
        }
        stats.put("path", getAppContext().getDetectedPath());
        return stats;
    }

    @Override
    public Map<String, String> monitor(String id) {
        return show(id);
    }
}
