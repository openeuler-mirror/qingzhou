package qingzhou.app.demo;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(code = "os", order = 2,
        name = {"操作系统", "en:Operating System"},
        info = {"操作系统运行状态监控", "en:Operating system runtime monitoring"},
        icon = "Monitor",
        menu = "monitor",
        action = "monitor")
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
            group = {"CPU", "en:CPU"})
    public String cpuProcessUsage;

    @ModelField(
            name = {"系统CPU使用率(%)", "en:System CPU Usage (%)"},
            info = {"系统整体CPU使用率", "en:System overall CPU usage"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"CPU", "en:CPU"})
    public String cpuSystemUsage;

    @ModelField(
            name = {"总物理内存(MB)", "en:Total Physical Memory (MB)"},
            info = {"系统总物理内存", "en:Total physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String totalPhysicalMemory;

    @ModelField(
            name = {"空闲物理内存(MB)", "en:Free Physical Memory (MB)"},
            info = {"系统空闲物理内存", "en:Free physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String freePhysicalMemory;

    @ModelField(
            name = {"已用物理内存(MB)", "en:Used Physical Memory (MB)"},
            info = {"系统已用物理内存", "en:Used physical memory"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"内存", "en:Memory"})
    public String usedPhysicalMemory;

    @ModelField(
            name = {"总交换空间(MB)", "en:Total Swap Space (MB)"},
            info = {"系统总交换空间", "en:Total swap space"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"交换空间", "en:Swap"})
    public String totalSwapSpace;

    @ModelField(
            name = {"空闲交换空间(MB)", "en:Free Swap Space (MB)"},
            info = {"系统空闲交换空间", "en:Free swap space"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"交换空间", "en:Swap"})
    public String freeSwapSpace;

    @ModelField(
            name = {"打开文件描述符数", "en:Open File Descriptors"},
            info = {"当前打开的文件描述符数量", "en:Open file descriptor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"文件描述符", "en:File Descriptors"})
    public String openFileDescriptorCount;

    @ModelField(
            name = {"最大文件描述符数", "en:Max File Descriptors"},
            info = {"文件描述符上限", "en:Max file descriptor count"},
            field_type = FieldType.MONITORING,
            numeric = true,
            group = {"文件描述符", "en:File Descriptors"})
    public String maxFileDescriptorCount;

    @ModelField(
            name = {"可用处理器数", "en:Available Processors"},
            info = {"可用CPU核心数", "en:Available CPU cores"},
            field_type = FieldType.MONITORING)
    public String availableProcessors;

    @ModelField(
            name = {"系统负载均值", "en:System Load Average"},
            info = {"系统1分钟负载均值", "en:System 1-minute load average"},
            field_type = FieldType.MONITORING,
            numeric = true)
    public String systemLoadAverage;

    @Override
    public Map<String, String> monitor(Request request) throws Exception {
        Map<String, String> data = new HashMap<>();

        data.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        data.put("availableProcessors", String.valueOf(osBean.getAvailableProcessors()));

        double loadAvg = osBean.getSystemLoadAverage();
        data.put("systemLoadAverage", loadAvg < 0 ? "-1" : String.format("%.2f", loadAvg));

        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;

            double processCpu = sunOsBean.getProcessCpuLoad();
            data.put("cpuProcessUsage", processCpu < 0 ? "-1" : String.format("%.2f", processCpu * 100));

            double systemCpu = sunOsBean.getSystemCpuLoad();
            data.put("cpuSystemUsage", systemCpu < 0 ? "-1" : String.format("%.2f", systemCpu * 100));

            long totalMem = sunOsBean.getTotalPhysicalMemorySize();
            long freeMem = sunOsBean.getFreePhysicalMemorySize();
            data.put("totalPhysicalMemory", String.valueOf(totalMem / (1024 * 1024)));
            data.put("freePhysicalMemory", String.valueOf(freeMem / (1024 * 1024)));
            data.put("usedPhysicalMemory", String.valueOf((totalMem - freeMem) / (1024 * 1024)));

            long totalSwap = sunOsBean.getTotalSwapSpaceSize();
            long freeSwap = sunOsBean.getFreeSwapSpaceSize();
            data.put("totalSwapSpace", String.valueOf(totalSwap / (1024 * 1024)));
            data.put("freeSwapSpace", String.valueOf(freeSwap / (1024 * 1024)));
        } else {
            data.put("cpuProcessUsage", "-1");
            data.put("cpuSystemUsage", "-1");
            data.put("totalPhysicalMemory", "-1");
            data.put("freePhysicalMemory", "-1");
            data.put("usedPhysicalMemory", "-1");
            data.put("totalSwapSpace", "-1");
            data.put("freeSwapSpace", "-1");
        }

        if (osBean instanceof com.sun.management.UnixOperatingSystemMXBean) {
            com.sun.management.UnixOperatingSystemMXBean unixOsBean = (com.sun.management.UnixOperatingSystemMXBean) osBean;
            data.put("openFileDescriptorCount", String.valueOf(unixOsBean.getOpenFileDescriptorCount()));
            data.put("maxFileDescriptorCount", String.valueOf(unixOsBean.getMaxFileDescriptorCount()));
        } else {
            data.put("openFileDescriptorCount", "-1");
            data.put("maxFileDescriptorCount", "-1");
        }

        return data;
    }
}
