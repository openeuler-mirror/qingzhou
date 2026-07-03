package qingzhou.app.redis.collector;

import qingzhou.app.redis.store.MetricsStore;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

public class MachineCollector {

    private final MetricsStore metricsStore;

    public MachineCollector(MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    public void collect() {
        try {
            long now = System.currentTimeMillis();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            double processCpu = invokeDoubleMethod(osBean, "com.sun.management.OperatingSystemMXBean", "getProcessCpuLoad");
            if (processCpu >= 0) {
                metricsStore.record("machine.process_cpu_usage", processCpu * 100, now);
            }

            double systemCpu = invokeDoubleMethod(osBean, "com.sun.management.OperatingSystemMXBean", "getSystemCpuLoad");
            if (systemCpu >= 0) {
                metricsStore.record("machine.system_cpu_usage", systemCpu * 100, now);
            }

            long totalMem = invokeLongMethod(osBean, "com.sun.management.OperatingSystemMXBean", "getTotalPhysicalMemorySize");
            long freeMem = invokeLongMethod(osBean, "com.sun.management.OperatingSystemMXBean", "getFreePhysicalMemorySize");
            if (totalMem >= 0 && freeMem >= 0) {
                long usedMem = totalMem - freeMem;
                metricsStore.record("machine.used_physical_memory_mb", usedMem / (1024 * 1024), now);
                metricsStore.record("machine.free_physical_memory_mb", freeMem / (1024 * 1024), now);
                metricsStore.record("machine.memory_usage_rate", usedMem * 100.0 / totalMem, now);
            }

            double loadAvg = osBean.getSystemLoadAverage();
            if (loadAvg >= 0) {
                metricsStore.record("machine.system_load_average", loadAvg, now);
            }

            long openFd = invokeLongMethod(osBean, "com.sun.management.UnixOperatingSystemMXBean", "getOpenFileDescriptorCount");
            if (openFd >= 0) {
                metricsStore.record("machine.open_file_descriptor_count", openFd, now);
            }
        } catch (Exception e) {
        }
    }

    private double invokeDoubleMethod(Object obj, String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isInstance(obj)) {
                return -1;
            }
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(obj);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private long invokeLongMethod(Object obj, String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!clazz.isInstance(obj)) {
                return -1;
            }
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(obj);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
