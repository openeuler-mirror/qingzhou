package qingzhou.app.common.monitor;


import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.MonitorModel;
import qingzhou.framework.util.ServerUtil;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Model(name = "jvm",
        icon = "coffee", nameI18n = {"JVM", "en:JVM"},
        infoI18n = {"描述 Java 虚拟机（JVM）的版本、厂商等基本信息，以及Java进程的堆内存、非堆内存等使用情况。",
                "en:Describes basic information such as the version and manufacturer of the Java Virtual Machine (JVM), as well as the usage of heap memory and non-heap memory of the Java process."})
public class Jvm extends ModelBase implements MonitorModel {
    @ModelField(isMonitorField = true, nameI18n = {"名称", "en:Name"}, infoI18n = {"Java 虚拟机规范名称。", "en:Java virtual machine specification name."})
    private String SpecName;

    @ModelField(isMonitorField = true, nameI18n = {"版本", "en:Version"}, infoI18n = {"Java 虚拟机规范版本。", "en:Java virtual machine specification version."})
    private String SpecVersion;

    @ModelField(isMonitorField = true, nameI18n = {"JVM 软件名称", "en:JVM Software Name"}, infoI18n = {"Java 虚拟机名称。", "en:The Java virtual machine implementation name."})
    private String VmName;

    @ModelField(isMonitorField = true, nameI18n = {"JVM 软件供应商", "en:JVM Software Version"}, infoI18n = {"Java 虚拟机供应商。", "en:JVM software vendor."})
    private String VmVendor;

    @ModelField(isMonitorField = true, nameI18n = {"JVM 软件版本号", "en:JVM Software Vendor"}, infoI18n = {"Java 虚拟机版本。", "en:JVM software version."})
    private String VmVersion;

    @ModelField(isMonitorField = true, nameI18n = {"进程信息", "en:Process Name"}, infoI18n = {"当前运行服务的操作系统PID。", "en:PID of the operating system currently running the service."})
    private String Name;

    @ModelField(isMonitorField = true, nameI18n = {"启动时间", "en:Start Time"}, infoI18n = {"Java 虚拟机的启动时间。", "en:The Java virtual machine startup time."})
    private String StartTime;

    @ModelField(isMonitorField = true, nameI18n = {"引导类路径", "en:Boot Class Path"}, infoI18n = {"引导类加载器用来搜索类文件的引导类路径。", "en:The boot class path that is used by the bootstrap class loader."})
    private String BootClassPath = "";// null on jvm 14

    @ModelField(isMonitorField = true, nameI18n = {"系统类路径", "en:Class Path"}, infoI18n = {"系统类加载器用来搜索类文件的Java类路径。", "en:The Java class path that is used by the system class loader."})
    private String ClassPath;

    @ModelField(isMonitorField = true, nameI18n = {"本地库路径", "en:Library Path"}, infoI18n = {"Java库路径中的多个路径由要监视的 Java 虚拟机平台的路径分隔符分隔。", "en:Multiple paths in the Java library path are separated by the path separator character of the platform of the Java virtual machine being monitored."})
    private String LibraryPath;

    @ModelField(isMonitorField = true, nameI18n = {"启动参数", "en:Input Arguments"}, infoI18n = {"传递给 Java 虚拟机的输入参数。", "en:The input arguments passed to the Java virtual machine."})
    private String InputArguments;

    @ModelField(isMonitorField = true,
            nameI18n = {"最大堆内存（MB）", "en:Heap Memory Max (MB)"},
            infoI18n = {"可使用的最大堆内存，单位MB。", "en:The maximum heap memory that can be used, in MB."})
    public Double heapCommitted;

    @ModelField(isMonitorField = true, nameI18n = {"JVM 线程总数", "en:JVM Thread Count"},
            supportGraphical = true,
            infoI18n = {"当前活动线程的数量，包括守护线程和非守护线程。", "en:The current number of live threads including both daemon and non-daemon threads."})
    private int threadCount;

    @ModelField(isMonitorField = true, nameI18n = {"死锁线程数", "en:Deadlocked Threads"},
            supportGraphical = true,
            infoI18n = {"死锁等待对象监视器或同步器的线程数。", "en:The number of threads deadlocked waiting for an object monitor or synchronizer."})
    private int deadlockedThreadCount;

    @ModelField(isMonitorField = true, supportGraphical = true,
            nameI18n = {"使用中堆内存（MB）", "en:Heap Memory Used (MB)"},
            infoI18n = {"正在使用的堆内存的大小，单位MB。", "en:The size of the heap memory in use, in MB."})
    public Double heapUsed;

    @ModelField(isMonitorField = true, supportGraphical = true,
            nameI18n = {"使用中非堆内存（MB）", "en:Non-Heap Memory Used (MB)"},
            infoI18n = {"正在使用的非堆内存的大小，单位MB。", "en:The size of the non-heap memory in use, in MB."})
    public Double nonHeapUsed;

    @ModelField(isMonitorField = true, supportGraphicalDynamic = true, nameI18n = {"收集总数", "en:GC Count"},
            infoI18n = {"垃圾收集发生的次数。", "en:The total number of collections that have occurred."})
    private Integer collectionCount = 0;

    @ModelField(isMonitorField = true, supportGraphicalDynamic = true, nameI18n = {"GC时间(毫秒)", "en:GC Time (milliseconds)"},
            infoI18n = {"垃圾收集的累积时间（以毫秒为单位）。", "en:The approximate accumulated collection elapsed time in milliseconds."})
    private Integer collectionTime = 0;

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
        properties.put("heapUsed", ServerUtil.maskMBytes(memoryMXBean.getHeapMemoryUsage().getUsed()));
        properties.put("heapCommitted", ServerUtil.maskMBytes(memoryMXBean.getHeapMemoryUsage().getCommitted()));

        properties.put("nonHeapUsed", ServerUtil.maskMBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed()));

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = Arrays.toString(gcBean.getMemoryPoolNames());
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            properties.put("collectionCount" + MONITOR_EXT_SEPARATOR + name, String.valueOf(count));
            properties.put("collectionTime" + MONITOR_EXT_SEPARATOR + name, String.valueOf(time)); // 最大减去可用就是当前使用中的
        }

        return properties;
    }

    private static Map<String, String> basicProperties;

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
        try {
            data.put("BootClassPath", mxBean.getBootClassPath());
        } catch (Exception e) {
            // jdk14: Boot class path mechanism is not supported
        }

        data.put("ClassPath", rectifyPath(mxBean.getClassPath()));
        data.put("LibraryPath", rectifyPath(mxBean.getLibraryPath()));

//        properties.put("InputArguments", mxBean.getInputArguments().toString());// ITAIT-2658 不能格式化： json 返回时候不能带有中括号 []
        StringBuilder args = new StringBuilder();
        for (String s : mxBean.getInputArguments()) {
            args.append(s).append(" ");
        }
        data.put("InputArguments", rectifyPath(args.toString()));

        basicProperties = data;
        return basicProperties;
    }

    private String rectifyPath(String originPath) {
        StringBuilder path = new StringBuilder();
        for (String p : originPath.split(File.pathSeparator)) {
            try {
                File file = new File(p);
                if (file.exists()) {
                    path.append(file.getCanonicalPath());
                } else {
                    path.append(p);
                }
            } catch (Exception e) {
                path.append(p);
            }
            path.append(File.pathSeparator);
        }
        String result = path.toString();
        try {
            result = result.replace(ServerUtil.getDomain().getCanonicalPath(), "${tongweb.base}");
            result = result.replace(ServerUtil.getHome().getCanonicalPath(), "${tongweb.home}");
        } catch (Exception ignored) {
        }
        return result;
    }
}