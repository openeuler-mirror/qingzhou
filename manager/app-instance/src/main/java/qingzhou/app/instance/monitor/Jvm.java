package qingzhou.app.instance.monitor;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitorable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(code = "jvm", icon = "coffee", menu = "Monitor",
        order = 1, entrance = Monitorable.ACTION_NAME_MONITOR,
        name = {"JVM", "en:JVM"},
        info = {"描述 Java 虚拟机（JVM）的版本、厂商等基本信息，以及Java进程的堆内存、非堆内存等使用情况。",
                "en:Describes basic information such as the version and manufacturer of the Java Virtual Machine (JVM), as well as the usage of heap memory and non-heap memory of the Java process."})
public class Jvm extends ModelBase implements Monitorable {
    @ModelField(name = {"名称", "en:Name"}, info = {"Java 虚拟机规范名称。", "en:Java virtual machine specification name."})
    public String SpecName;

    @ModelField(name = {"版本", "en:Version"}, info = {"Java 虚拟机规范版本。", "en:Java virtual machine specification version."})
    public String SpecVersion;

    @ModelField(name = {"JVM 软件名称", "en:JVM Software Name"}, info = {"Java 虚拟机名称。", "en:The Java virtual machine implementation name."})
    public String VmName;

    @ModelField(name = {"JVM 软件供应商", "en:JVM Software Version"}, info = {"Java 虚拟机供应商。", "en:JVM software vendor."})
    public String VmVendor;

    @ModelField(monitor = true, name = {"JVM 软件版本号", "en:JVM Software Vendor"}, info = {"Java 虚拟机版本。", "en:JVM software version."})
    public String VmVersion;

    @ModelField(monitor = true, name = {"进程信息", "en:Process Name"}, info = {"当前运行服务的操作系统PID。", "en:PID of the operating system currently running the service."})
    public String Name;

    @ModelField(monitor = true, name = {"启动时间", "en:Start Time"}, info = {"Java 虚拟机的启动时间。", "en:The Java virtual machine startup time."})
    public String StartTime;

    @ModelField(
            monitor = true, numeric = true,
            name = {"最大堆内存（MB）", "en:Heap Memory Max (MB)"},
            info = {"可使用的最大堆内存，单位MB。", "en:The maximum heap memory that can be used, in MB."})
    public double heapCommitted;

    @ModelField(monitor = true, numeric = true, name = {"JVM 线程总数", "en:JVM Thread Count"}, info = {"当前活动线程的数量，包括守护线程和非守护线程。", "en:The current number of live threads including both daemon and non-daemon threads."})
    public int threadCount;

    @ModelField(monitor = true, numeric = true, name = {"死锁线程数", "en:Deadlocked Threads"}, info = {"死锁等待对象监视器或同步器的线程数。", "en:The number of threads deadlocked waiting for an object monitor or synchronizer."})
    public int deadlockedThreadCount;

    @ModelField(monitor = true, numeric = true, name = {"使用中堆内存（MB）", "en:Heap Memory Used (MB)"}, info = {"正在使用的堆内存的大小，单位MB。", "en:The size of the heap memory in use, in MB."})
    public double heapUsed;

    @ModelField(monitor = true, numeric = true, name = {"使用中非堆内存（MB）", "en:Non-Heap Memory Used (MB)"}, info = {"正在使用的非堆内存的大小，单位MB。", "en:The size of the non-heap memory in use, in MB."})
    public double nonHeapUsed;

    @Override
    public Map<String, String> monitorData() {
        Map<String, String> properties = new HashMap<>(getBasic());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadMXBean.getThreadCount();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        properties.put("threadCount", String.valueOf(threadCount));
        int deadlockedThreadCount = 0;
        if (deadlockedThreads != null) {
            deadlockedThreadCount = deadlockedThreads.length;
        }
        properties.put("deadlockedThreadCount", String.valueOf(deadlockedThreadCount));

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        properties.put("heapUsed", convertMBytes(memoryMXBean.getHeapMemoryUsage().getUsed()));
        properties.put("heapCommitted", convertMBytes(memoryMXBean.getHeapMemoryUsage().getCommitted()));

        properties.put("nonHeapUsed", convertMBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        return properties;
    }

    private String convertMBytes(long val) {
        double v = ((double) val) / 1024 / 1024;
        DecimalFormat df = new DecimalFormat("##0.0");//这样为保持1位
        return df.format(v);
    }

    private Map<String, String> basicProperties;

    private Map<String, String> getBasic() {
        if (basicProperties != null) {
            return basicProperties;
        }

        Map<String, String> data = new HashMap<>();
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        data.put("SpecName", mxBean.getSpecName());
        data.put("SpecVersion", mxBean.getSpecVersion());
        data.put("VmName", mxBean.getVmName());
        data.put("VmVendor", mxBean.getVmVendor());
        data.put("VmVersion", mxBean.getVmVersion());
        data.put("Name", mxBean.getName());

        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(mxBean.getStartTime()));
        data.put("StartTime", format);

        basicProperties = data;
        return basicProperties;
    }
}