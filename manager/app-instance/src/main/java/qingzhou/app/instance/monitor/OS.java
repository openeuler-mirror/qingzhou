package qingzhou.app.instance.monitor;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitorable;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@Model(code = "os", icon = "desktop", menu = "Monitor",
        name = {"操作系统", "en:Operating System"}, hidden = true,
        info = {"操作系统的基本信息。", "en:Basic information about the operating system."})
public class OS extends ModelBase implements Monitorable {

    @ModelField(name = {"名称", "en:Operating System Name"}, info = {"操作系统名称。", "en:Operating system name."})
    public String Name;

    @ModelField(name = {"版本", "en:Operating System Version"}, info = {"操作系统的版本号。", "en:Operating system version."})
    private String Version;

    @ModelField(name = {"架构", "en:Operating System Architecture"}, info = {"操作系统所基于的硬件架构。", "en:Operating system architecture."})
    private String Arch;

    @ModelField(name = {"CPU 个数", "en:The Number Of Cpus"}, info = {"虚拟机可用的处理器数量。", "en:The number of processors available to the virtual machine."})
    public int AvailableProcessors;

    @ModelField(
            monitor = true, numeric = true,
            name = {"CPU负载", "en:Load Average"},
            info = {"表示当前系统 CPU 的总体利用率，范围在 0.0 ~ 1.0 之间。当返回值为 0.0 时，意味着当前系统 CPU 没有被占用，即所有的 CPU 核心都处于空闲状态。这通常表示系统处于比较空闲的状态，没有太多的 CPU 密集型任务在运行。", "en:Represents the overall utilization rate of the current system CPU, ranging from 0.0 to 1.0. When the return value is 0.0, it means that the current system CPU is not occupied, that is, all CPU cores are idle. This usually indicates that the system is in a relatively idle state and there are not many CPU intensive tasks running."})
    public Double SystemCpuLoad = 0.0;

    @ModelField(monitor = true, numeric = true, name = {"总物理内存（GB）", "en:Total Physical Memory (GB)"},
            info = {"操作系统的总物理内存，单位GB。", "en:The total physical memory of the operating system, in GB."})
    public Double TotalPhysicalMemorySize;

    @ModelField(monitor = true, numeric = true, name = {"空闲物理内存（GB）", "en:Free Physical Memory (GB)"}, info = {"操作系统的空闲物理内存，单位GB。", "en:Free physical memory of operating system, in GB."})
    public Double FreePhysicalMemorySize;

    @ModelField(monitor = true, numeric = true, name = {"总交换空间（GB）", "en:Total Exchange Space (GB)"}, info = {"操作系统的总交换空间，单位GB。", "en:Total exchange space of operating system, in GB."})
    public Double TotalSwapSpaceSize;

    @ModelField(monitor = true, numeric = true, name = {"空闲交换空间（GB）", "en:Free Swap Space (GB)"}, info = {"操作系统的空闲交换空间，单位GB。", "en:Free swap space of operating system, in GB."})
    public Double FreeSwapSpaceSize;

    @ModelField(monitor = true, numeric = true, name = {"当前虚拟内存（GB）", "en:Current Virtual Memory (GB)"}, info = {"操作系统的当前虚拟内存，单位GB。", "en:Current virtual memory of operating system, in GB."})
    private Double CommittedVirtualMemorySize;

    @ModelField(monitor = true, numeric = true, name = {"磁盘分区总量（GB）", "en:File Total Space (GB)"},
            info = {"TongWeb 所在的磁盘分区总空间大小，单位GB。", "en:The total space of the disk partition where TongWeb is located, in GB."})
    public Double fileTotalSpace;

    @ModelField(monitor = true, numeric = true, name = {"磁盘分区剩余（GB）", "en:File Free Space (GB)"},
            info = {"TongWeb 所在的磁盘分区剩余空间，单位GB。", "en:The remaining space of the disk partition where TongWeb is located, in GB."})
    public Double fileFreeSpace;

    @Override
    public Map<String, String> monitorData() {
        Map<String, String> map = new HashMap<>();
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        map.put("Name", mxBean.getName());
        map.put("Version", mxBean.getVersion());
        map.put("Arch", mxBean.getArch());
        map.put("AvailableProcessors", String.valueOf(mxBean.getAvailableProcessors()));

        long TotalPhysicalMemorySize = -1;
        long FreePhysicalMemorySize = -1;
        long TotalSwapSpaceSize = -1;
        long FreeSwapSpaceSize = -1;
        long CommittedVirtualMemorySize = -1;

        try {
            com.sun.management.OperatingSystemMXBean sunMXBean;
            if (mxBean instanceof com.sun.management.OperatingSystemMXBean) {
                sunMXBean = (com.sun.management.OperatingSystemMXBean) mxBean;
                TotalPhysicalMemorySize = sunMXBean.getTotalPhysicalMemorySize();
                FreePhysicalMemorySize = sunMXBean.getFreePhysicalMemorySize();
                TotalSwapSpaceSize = sunMXBean.getTotalSwapSpaceSize();
                FreeSwapSpaceSize = sunMXBean.getFreeSwapSpaceSize();
                CommittedVirtualMemorySize = sunMXBean.getCommittedVirtualMemorySize();
            }
        } catch (Throwable ignored) {
        }
        map.put("TotalPhysicalMemorySize", convertGBytes(TotalPhysicalMemorySize));
        map.put("FreePhysicalMemorySize", convertGBytes(FreePhysicalMemorySize));
        map.put("TotalSwapSpaceSize", convertGBytes(TotalSwapSpaceSize));
        map.put("FreeSwapSpaceSize", convertGBytes(FreeSwapSpaceSize));
        map.put("CommittedVirtualMemorySize", convertGBytes(CommittedVirtualMemorySize));
        map.put("fileTotalSpace", convertGBytes(new File(".").getTotalSpace()));
        map.put("fileFreeSpace", convertGBytes(new File(".").getFreeSpace()));

        double v;
        if (mxBean.getSystemLoadAverage() < 0) {
            com.sun.management.OperatingSystemMXBean osmxb = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            v = osmxb.getSystemCpuLoad();
        } else {
            v = mxBean.getSystemLoadAverage() / mxBean.getAvailableProcessors();// mac 等系统
        }
        map.put("SystemCpuLoad", String.format("%.2f", v));// #ITAIT-3029

        return map;
    }

    private String convertGBytes(long val) {
        double v = ((double) val) / 1024 / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }
}
