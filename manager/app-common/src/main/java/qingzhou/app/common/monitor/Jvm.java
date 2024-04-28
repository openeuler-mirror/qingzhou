package qingzhou.app.common.monitor;


import qingzhou.api.Model;
import qingzhou.api.ModelBase;
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

@Model(code = "jvm", icon = "coffee",
        order = 2, entrance = Monitorable.ACTION_NAME_MONITOR,
        name = {"JVM", "en:JVM"},
        info = {"描述 Java 虚拟机（JVM）的版本、厂商等基本信息，以及Java进程的堆内存、非堆内存等使用情况。",
                "en:Describes basic information such as the version and manufacturer of the Java Virtual Machine (JVM), as well as the usage of heap memory and non-heap memory of the Java process."})
public class Jvm extends ModelBase implements Monitorable {
    public String SpecName;

    public String SpecVersion;

    public String VmName;

    public String VmVendor;

    public String VmVersion;

    public String Name;

    public String StartTime;

    public double heapCommitted;

    public int threadCount;

    public int deadlockedThreadCount;

    public double heapUsed;

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