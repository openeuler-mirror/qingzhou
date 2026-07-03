package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Monitor;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.util.HostInfoUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Model(code = "hostEnvironment", icon = "Platform", menu = "redis-ops", order = 1,
        name = {"宿主环境", "en:Host Environment"},
        info = {"应用宿主机器的运行环境与资源概况", "en:Host machine environment and resource overview"})
public class HostEnvironment extends RedisModelBase implements Monitor {

    @ModelField(field_type = FieldType.MONITORING,
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"})
    public String statsTime;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"操作系统", "en:OS Name"},
            info = {"操作系统名称", "en:Operating system name"})
    public String osName;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"OS 版本", "en:OS Version"},
            info = {"操作系统版本", "en:Operating system version"})
    public String osVersion;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"系统架构", "en:Architecture"},
            info = {"操作系统架构", "en:Operating system architecture"})
    public String osArch;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"CPU 核心数", "en:CPU Cores"},
            info = {"可用 CPU 核心数", "en:Available CPU cores"})
    public String availableProcessors;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.gauge, group = "cpu",
            name = {"进程 CPU 使用率(%)", "en:Process CPU Usage (%)"},
            info = {"当前进程 CPU 使用率", "en:Current process CPU usage"})
    public String cpuProcessUsage;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.gauge, group = "cpu",
            name = {"系统 CPU 使用率(%)", "en:System CPU Usage (%)"},
            info = {"系统整体 CPU 使用率", "en:System overall CPU usage"})
    public String cpuSystemUsage;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "memory",
            name = {"总物理内存(MB)", "en:Total Physical Memory (MB)"},
            info = {"系统总物理内存", "en:Total physical memory"})
    public String totalPhysicalMemory;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "memory",
            name = {"已用物理内存(MB)", "en:Used Physical Memory (MB)"},
            info = {"系统已用物理内存", "en:Used physical memory"})
    public String usedPhysicalMemory;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "memory",
            name = {"空闲物理内存(MB)", "en:Free Physical Memory (MB)"},
            info = {"系统空闲物理内存", "en:Free physical memory"})
    public String freePhysicalMemory;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "swap",
            name = {"总交换空间(MB)", "en:Total Swap Space (MB)"},
            info = {"系统总交换空间", "en:Total swap space"})
    public String totalSwapSpace;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "swap",
            name = {"空闲交换空间(MB)", "en:Free Swap Space (MB)"},
            info = {"系统空闲交换空间", "en:Free swap space"})
    public String freeSwapSpace;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.bar, group = "file",
            name = {"打开文件描述符数", "en:Open File Descriptors"},
            info = {"当前打开的文件描述符数量", "en:Open file descriptor count"})
    public String openFileDescriptorCount;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.bar, group = "file",
            name = {"最大文件描述符数", "en:Max File Descriptors"},
            info = {"文件描述符上限", "en:Max file descriptor count"})
    public String maxFileDescriptorCount;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line,
            name = {"系统负载均值", "en:System Load Average"},
            info = {"系统 1 分钟负载均值", "en:System 1-minute load average"})
    public String systemLoadAverage;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"Java 版本", "en:Java Version"},
            info = {"运行时 Java 版本", "en:Runtime Java version"})
    public String javaVersion;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"进程 PID", "en:Process PID"},
            info = {"当前进程 PID", "en:Current process PID"})
    public String processPid;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"主机名", "en:Hostname"},
            info = {"当前主机名", "en:Current hostname"})
    public String hostname;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        data.put("osName", HostInfoUtil.getOsName());
        data.put("osVersion", HostInfoUtil.getOsVersion());
        data.put("osArch", HostInfoUtil.getOsArch());
        data.put("availableProcessors", String.valueOf(HostInfoUtil.getAvailableProcessors()));

        double processCpu = HostInfoUtil.getProcessCpuUsage();
        data.put("cpuProcessUsage", processCpu < 0 ? "-1" : String.format("%.2f", processCpu * 100));
        double systemCpu = HostInfoUtil.getSystemCpuUsage();
        data.put("cpuSystemUsage", systemCpu < 0 ? "-1" : String.format("%.2f", systemCpu * 100));

        long totalMem = HostInfoUtil.getTotalPhysicalMemory();
        long freeMem = HostInfoUtil.getFreePhysicalMemory();
        data.put("totalPhysicalMemory", totalMem < 0 ? "-1" : String.valueOf(totalMem / (1024 * 1024)));
        data.put("freePhysicalMemory", freeMem < 0 ? "-1" : String.valueOf(freeMem / (1024 * 1024)));
        data.put("usedPhysicalMemory", totalMem < 0 || freeMem < 0 ? "-1" : String.valueOf((totalMem - freeMem) / (1024 * 1024)));

        long totalSwap = HostInfoUtil.getTotalSwapSpace();
        long freeSwap = HostInfoUtil.getFreeSwapSpace();
        data.put("totalSwapSpace", totalSwap < 0 ? "-1" : String.valueOf(totalSwap / (1024 * 1024)));
        data.put("freeSwapSpace", freeSwap < 0 ? "-1" : String.valueOf(freeSwap / (1024 * 1024)));

        long openFd = HostInfoUtil.getOpenFileDescriptorCount();
        long maxFd = HostInfoUtil.getMaxFileDescriptorCount();
        data.put("openFileDescriptorCount", openFd < 0 ? "-1" : String.valueOf(openFd));
        data.put("maxFileDescriptorCount", maxFd < 0 ? "-1" : String.valueOf(maxFd));

        double load = HostInfoUtil.getSystemLoadAverage();
        data.put("systemLoadAverage", load < 0 ? "-1" : String.format("%.2f", load));
        data.put("javaVersion", HostInfoUtil.getJavaVersion());
        data.put("processPid", HostInfoUtil.getProcessPid());
        data.put("hostname", HostInfoUtil.getHostname());

        return data;
    }
}
