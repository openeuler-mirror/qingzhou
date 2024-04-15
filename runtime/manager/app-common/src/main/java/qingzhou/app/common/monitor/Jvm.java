package qingzhou.app.common.monitor;


import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.MonitorField;
import qingzhou.api.type.Monitorable;
import qingzhou.engine.util.StringUtil;

import java.lang.management.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(name = "jvm", icon = "coffee",
        menuOrder = 2, entryAction = Monitorable.ACTION_NAME_MONITOR,
        nameI18n = {"JVM", "en:JVM"},
        infoI18n = {"描述 Java 虚拟机（JVM）的版本、厂商等基本信息，以及Java进程的堆内存、非堆内存等使用情况。",
                "en:Describes basic information such as the version and manufacturer of the Java Virtual Machine (JVM), as well as the usage of heap memory and non-heap memory of the Java process."})
public class Jvm extends ModelBase implements Monitorable {
    @MonitorField(dynamic = false, nameI18n = {"名称", "en:Name"}, infoI18n = {"Java 虚拟机规范名称。", "en:Java virtual machine specification name."})
    public String SpecName;

    @MonitorField(dynamic = false, nameI18n = {"版本", "en:Version"}, infoI18n = {"Java 虚拟机规范版本。", "en:Java virtual machine specification version."})
    public String SpecVersion;

    @MonitorField(dynamic = false, nameI18n = {"JVM 软件名称", "en:JVM Software Name"}, infoI18n = {"Java 虚拟机名称。", "en:The Java virtual machine implementation name."})
    public String VmName;

    @MonitorField(dynamic = false, nameI18n = {"JVM 软件供应商", "en:JVM Software Version"}, infoI18n = {"Java 虚拟机供应商。", "en:JVM software vendor."})
    public String VmVendor;

    @MonitorField(dynamic = false, nameI18n = {"JVM 软件版本号", "en:JVM Software Vendor"}, infoI18n = {"Java 虚拟机版本。", "en:JVM software version."})
    public String VmVersion;

    @MonitorField(dynamic = false, nameI18n = {"进程信息", "en:Process Name"}, infoI18n = {"当前运行服务的操作系统PID。", "en:PID of the operating system currently running the service."})
    public String Name;

    @MonitorField(dynamic = false, nameI18n = {"启动时间", "en:Start Time"}, infoI18n = {"Java 虚拟机的启动时间。", "en:The Java virtual machine startup time."})
    public String StartTime;

    @MonitorField(dynamic = false,
            nameI18n = {"最大堆内存（MB）", "en:Heap Memory Max (MB)"},
            infoI18n = {"可使用的最大堆内存，单位MB。", "en:The maximum heap memory that can be used, in MB."})
    public double heapCommitted;

    @MonitorField(nameI18n = {"JVM 线程总数", "en:JVM Thread Count"},
            infoI18n = {"当前活动线程的数量，包括守护线程和非守护线程。", "en:The current number of live threads including both daemon and non-daemon threads."})
    public int threadCount;

    @MonitorField(nameI18n = {"死锁线程数", "en:Deadlocked Threads"},
            infoI18n = {"死锁等待对象监视器或同步器的线程数。", "en:The number of threads deadlocked waiting for an object monitor or synchronizer."})
    public int deadlockedThreadCount;

    @MonitorField(
            nameI18n = {"使用中堆内存（MB）", "en:Heap Memory Used (MB)"},
            infoI18n = {"正在使用的堆内存的大小，单位MB。", "en:The size of the heap memory in use, in MB."})
    public double heapUsed;

    @MonitorField(
            nameI18n = {"使用中非堆内存（MB）", "en:Non-Heap Memory Used (MB)"},
            infoI18n = {"正在使用的非堆内存的大小，单位MB。", "en:The size of the non-heap memory in use, in MB."})
    public double nonHeapUsed;

    @MonitorField(dynamicMultiple = true,
            nameI18n = {"收集总数", "en:GC Count"},
            infoI18n = {"垃圾收集发生的次数。", "en:The total number of collections that have occurred."})
    public int collectionCount = 0;

    @MonitorField(dynamicMultiple = true,
            nameI18n = {"GC时间(毫秒)", "en:GC Time (milliseconds)"},
            infoI18n = {"垃圾收集的累积时间（以毫秒为单位）。", "en:The approximate accumulated collection elapsed time in milliseconds."})
    public int collectionTime = 0;

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
        properties.put("heapUsed", StringUtil.convertMBytes(memoryMXBean.getHeapMemoryUsage().getUsed()));
        properties.put("heapCommitted", StringUtil.convertMBytes(memoryMXBean.getHeapMemoryUsage().getCommitted()));

        properties.put("nonHeapUsed", StringUtil.convertMBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = Arrays.toString(gcBean.getMemoryPoolNames());
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            properties.put("collectionCount" + Monitorable.MONITOR_EXT_SEPARATOR + name, String.valueOf(count));
            properties.put("collectionTime" + Monitorable.MONITOR_EXT_SEPARATOR + name, String.valueOf(time)); // 最大减去可用就是当前使用中的
        }

        return properties;
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