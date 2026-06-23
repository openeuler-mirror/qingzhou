package qingzhou.app.demo;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitor;

@Model(code = "os", order = 2,
        name = {"操作系统", "en:Operating System"},
        info = {"操作系统运行状态监控", "en:Operating system runtime monitoring"},
        icon = "Monitor",
        menu = "monitor")
public class OsMonitor extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.MONITORING)
    public String statsTime;

    @ModelField(
            name = {"进程CPU使用率(%)", "en:Process CPU Usage (%)"},
            info = {"当前进程CPU使用率", "en:Current process CPU usage"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "cpu")
    public String cpuProcessUsage;

    @ModelField(
            name = {"系统CPU使用率(%)", "en:System CPU Usage (%)"},
            info = {"系统整体CPU使用率", "en:System overall CPU usage"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.gauge,
            group = "cpu")
    public String cpuSystemUsage;

    @ModelField(
            name = {"总物理内存(MB)", "en:Total Physical Memory (MB)"},
            info = {"系统总物理内存", "en:Total physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line,
            group = "memory")
    public String totalPhysicalMemory;

    @ModelField(
            name = {"空闲物理内存(MB)", "en:Free Physical Memory (MB)"},
            info = {"系统空闲物理内存", "en:Free physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line,
            group = "memory")
    public String freePhysicalMemory;

    @ModelField(
            name = {"已用物理内存(MB)", "en:Used Physical Memory (MB)"},
            info = {"系统已用物理内存", "en:Used physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line,
            group = "memory")
    public String usedPhysicalMemory;

    @ModelField(
            name = {"总交换空间(MB)", "en:Total Swap Space (MB)"},
            info = {"系统总交换空间", "en:Total swap space"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line,
            group = "swap")
    public String totalSwapSpace;

    @ModelField(
            name = {"空闲交换空间(MB)", "en:Free Swap Space (MB)"},
            info = {"系统空闲交换空间", "en:Free swap space"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line,
            group = "swap")
    public String freeSwapSpace;

    @ModelField(
            name = {"打开文件描述符数", "en:Open File Descriptors"},
            info = {"当前打开的文件描述符数量", "en:Open file descriptor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.bar,
            group = "file")
    public String openFileDescriptorCount;

    @ModelField(
            name = {"最大文件描述符数", "en:Max File Descriptors"},
            info = {"文件描述符上限", "en:Max file descriptor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.bar,
            group = "file")
    public String maxFileDescriptorCount;

    @ModelField(
            name = {"可用处理器数", "en:Available Processors"},
            info = {"可用CPU核心数", "en:Available CPU cores"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.stat)
    public String availableProcessors;

    @ModelField(
            name = {"系统负载均值", "en:System Load Average"},
            info = {"系统1分钟负载均值", "en:System 1-minute load average"},
            field_type = FieldType.MONITORING,
            numeric = true,
            chart_type = ChartType.line)
    public String systemLoadAverage;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        Map<String, String> data = new HashMap<>();

        data.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        data.put("availableProcessors", String.valueOf(osBean.getAvailableProcessors()));

        double loadAvg = osBean.getSystemLoadAverage();
        data.put("systemLoadAverage", loadAvg < 0 ? "-1" : String.format("%.2f", loadAvg));

        try {
            Class<?> sunOsClass = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (sunOsClass.isInstance(osBean)) {
                double processCpu = invokeDoubleMethod(osBean, sunOsClass, "getProcessCpuLoad");
                data.put("cpuProcessUsage", processCpu < 0 ? "-1" : String.format("%.2f", processCpu * 100));

                double systemCpu = invokeDoubleMethod(osBean, sunOsClass, "getSystemCpuLoad");
                data.put("cpuSystemUsage", systemCpu < 0 ? "-1" : String.format("%.2f", systemCpu * 100));

                long totalMem = invokeLongMethod(osBean, sunOsClass, "getTotalPhysicalMemorySize");
                long freeMem = invokeLongMethod(osBean, sunOsClass, "getFreePhysicalMemorySize");
                data.put("totalPhysicalMemory", String.valueOf(totalMem / (1024 * 1024)));
                data.put("freePhysicalMemory", String.valueOf(freeMem / (1024 * 1024)));
                data.put("usedPhysicalMemory", String.valueOf((totalMem - freeMem) / (1024 * 1024)));

                long totalSwap = invokeLongMethod(osBean, sunOsClass, "getTotalSwapSpaceSize");
                long freeSwap = invokeLongMethod(osBean, sunOsClass, "getFreeSwapSpaceSize");
                data.put("totalSwapSpace", String.valueOf(totalSwap / (1024 * 1024)));
                data.put("freeSwapSpace", String.valueOf(freeSwap / (1024 * 1024)));
            } else {
                setUnavailable(data);
            }
        } catch (ClassNotFoundException e) {
            setUnavailable(data);
        }

        try {
            Class<?> unixOsClass = Class.forName("com.sun.management.UnixOperatingSystemMXBean");
            if (unixOsClass.isInstance(osBean)) {
                long openFd = invokeLongMethod(osBean, unixOsClass, "getOpenFileDescriptorCount");
                long maxFd = invokeLongMethod(osBean, unixOsClass, "getMaxFileDescriptorCount");
                data.put("openFileDescriptorCount", String.valueOf(openFd));
                data.put("maxFileDescriptorCount", String.valueOf(maxFd));
            } else {
                data.put("openFileDescriptorCount", "-1");
                data.put("maxFileDescriptorCount", "-1");
            }
        } catch (ClassNotFoundException e) {
            data.put("openFileDescriptorCount", "-1");
            data.put("maxFileDescriptorCount", "-1");
        }

        return data;
    }

    private void setUnavailable(Map<String, String> data) {
        data.put("cpuProcessUsage", "-1");
        data.put("cpuSystemUsage", "-1");
        data.put("totalPhysicalMemory", "-1");
        data.put("freePhysicalMemory", "-1");
        data.put("usedPhysicalMemory", "-1");
        data.put("totalSwapSpace", "-1");
        data.put("freeSwapSpace", "-1");
    }

    private static double invokeDoubleMethod(Object obj, Class<?> clazz, String methodName) throws Exception {
        Method method = clazz.getMethod(methodName);
        Object result = method.invoke(obj);
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        return -1;
    }

    private static long invokeLongMethod(Object obj, Class<?> clazz, String methodName) throws Exception {
        Method method = clazz.getMethod(methodName);
        Object result = method.invoke(obj);
        if (result instanceof Number) {
            return ((Number) result).longValue();
        }
        return -1;
    }
}
