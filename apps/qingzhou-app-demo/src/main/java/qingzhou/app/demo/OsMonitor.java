package qingzhou.app.demo;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Monitor;

/**
 * 操作系统监控：基础指标走标准 {@link OperatingSystemMXBean}，扩展指标（CPU 负载、物理内存、文件描述符）
 * 通过反射调用 {@code com.sun.management} 平台专有 MXBean——该类在部分 JDK / 平台不存在，故用反射容错。
 */
@Model(code = "os", order = 2,
        name = {"操作系统", "en:Operating System"},
        info = {"操作系统运行状态监控", "en:Operating system runtime monitoring"},
        icon = "Monitor",
        menu = "monitor")
public class OsMonitor extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.monitor)
    public String statsTime;

    @ModelField(name = {"进程CPU使用率(%)", "en:Process CPU Usage (%)"}, info = {"当前进程CPU使用率", "en:Current process CPU usage"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge, group = "cpu")
    public String cpuProcessUsage;

    @ModelField(name = {"系统CPU使用率(%)", "en:System CPU Usage (%)"}, info = {"系统整体CPU使用率", "en:System overall CPU usage"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.gauge, group = "cpu")
    public String cpuSystemUsage;

    @ModelField(name = {"总物理内存(MB)", "en:Total Physical Memory (MB)"}, info = {"系统总物理内存", "en:Total physical memory"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String totalPhysicalMemory;

    @ModelField(name = {"空闲物理内存(MB)", "en:Free Physical Memory (MB)"}, info = {"系统空闲物理内存", "en:Free physical memory"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String freePhysicalMemory;

    @ModelField(name = {"已用物理内存(MB)", "en:Used Physical Memory (MB)"}, info = {"系统已用物理内存", "en:Used physical memory"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "memory")
    public String usedPhysicalMemory;

    @ModelField(name = {"总交换空间(MB)", "en:Total Swap Space (MB)"}, info = {"系统总交换空间", "en:Total swap space"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "swap")
    public String totalSwapSpace;

    @ModelField(name = {"空闲交换空间(MB)", "en:Free Swap Space (MB)"}, info = {"系统空闲交换空间", "en:Free swap space"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line, group = "swap")
    public String freeSwapSpace;

    @ModelField(name = {"打开文件描述符数", "en:Open File Descriptors"}, info = {"当前打开的文件描述符数量", "en:Open file descriptor count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "file")
    public String openFileDescriptorCount;

    @ModelField(name = {"最大文件描述符数", "en:Max File Descriptors"}, info = {"文件描述符上限", "en:Max file descriptor count"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.bar, group = "file")
    public String maxFileDescriptorCount;

    @ModelField(name = {"可用处理器数", "en:Available Processors"}, info = {"可用CPU核心数", "en:Available CPU cores"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.stat)
    public String availableProcessors;

    @ModelField(name = {"系统负载均值", "en:System Load Average"}, info = {"系统1分钟负载均值", "en:System 1-minute load average"},
            field_type = FieldType.monitor, numeric = true, chart_type = ChartType.line)
    public String systemLoadAverage;

    private static final int MB = 1024 * 1024;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        Map<String, String> d = new HashMap<>();
        d.put("statsTime", DateKit.now());

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        d.put("availableProcessors", String.valueOf(os.getAvailableProcessors()));

        // 采集不到时省略 key，禁止填 -1 哨兵：监控端点会把 -1 当作真实指标导出
        double loadAvg = os.getSystemLoadAverage();
        if (loadAvg >= 0) d.put("systemLoadAverage", String.format("%.2f", loadAvg));

        Class<?> sunOs = forName("com.sun.management.OperatingSystemMXBean");
        if (sunOs != null && sunOs.isInstance(os)) {
            Number processCpu = invoke(os, sunOs, "getProcessCpuLoad");
            if (processCpu != null && processCpu.doubleValue() >= 0)
                d.put("cpuProcessUsage", String.format("%.2f", processCpu.doubleValue() * 100));

            Number systemCpu = invoke(os, sunOs, "getSystemCpuLoad");
            if (systemCpu != null && systemCpu.doubleValue() >= 0)
                d.put("cpuSystemUsage", String.format("%.2f", systemCpu.doubleValue() * 100));

            Number totalMem = invoke(os, sunOs, "getTotalPhysicalMemorySize");
            Number freeMem = invoke(os, sunOs, "getFreePhysicalMemorySize");
            if (totalMem != null && freeMem != null) {
                d.put("totalPhysicalMemory", String.valueOf(totalMem.longValue() / MB));
                d.put("freePhysicalMemory", String.valueOf(freeMem.longValue() / MB));
                d.put("usedPhysicalMemory", String.valueOf((totalMem.longValue() - freeMem.longValue()) / MB));
            }

            Number totalSwap = invoke(os, sunOs, "getTotalSwapSpaceSize");
            Number freeSwap = invoke(os, sunOs, "getFreeSwapSpaceSize");
            if (totalSwap != null && freeSwap != null) {
                d.put("totalSwapSpace", String.valueOf(totalSwap.longValue() / MB));
                d.put("freeSwapSpace", String.valueOf(freeSwap.longValue() / MB));
            }
        }

        Class<?> unixOs = forName("com.sun.management.UnixOperatingSystemMXBean");
        if (unixOs != null && unixOs.isInstance(os)) {
            Number openFd = invoke(os, unixOs, "getOpenFileDescriptorCount");
            Number maxFd = invoke(os, unixOs, "getMaxFileDescriptorCount");
            if (openFd != null) d.put("openFileDescriptorCount", String.valueOf(openFd.longValue()));
            if (maxFd != null) d.put("maxFileDescriptorCount", String.valueOf(maxFd.longValue()));
        }

        return d;
    }

    private static Class<?> forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null; // 平台无该 MXBean 时相关字段整体缺失
        }
    }

    /** 反射调用平台专有 MXBean 的无参数值方法，返回 null 表示不可用。 */
    private static Number invoke(Object bean, Class<?> clazz, String method) throws Exception {
        Method m = clazz.getMethod(method);
        Object r = m.invoke(bean);
        return r instanceof Number ? (Number) r : null;
    }
}
