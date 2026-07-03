package qingzhou.app.redis.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;

public class HostInfoUtil {

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String getOsName() {
        return System.getProperty("os.name", "unknown");
    }

    public static String getOsVersion() {
        return System.getProperty("os.version", "unknown");
    }

    public static String getOsArch() {
        return System.getProperty("os.arch", "unknown");
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version", "unknown");
    }

    public static String getProcessPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }

    public static int getAvailableProcessors() {
        return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    public static double getSystemLoadAverage() {
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return load < 0 ? -1 : load;
    }

    public static double getProcessCpuUsage() {
        return invokeDouble("getProcessCpuLoad");
    }

    public static double getSystemCpuUsage() {
        return invokeDouble("getSystemCpuLoad");
    }

    public static long getTotalPhysicalMemory() {
        return invokeLong("getTotalPhysicalMemorySize");
    }

    public static long getFreePhysicalMemory() {
        return invokeLong("getFreePhysicalMemorySize");
    }

    public static long getTotalSwapSpace() {
        return invokeLong("getTotalSwapSpaceSize");
    }

    public static long getFreeSwapSpace() {
        return invokeLong("getFreeSwapSpaceSize");
    }

    public static long getOpenFileDescriptorCount() {
        return invokeUnixLong("getOpenFileDescriptorCount");
    }

    public static long getMaxFileDescriptorCount() {
        return invokeUnixLong("getMaxFileDescriptorCount");
    }

    private static double invokeDouble(String methodName) {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (!clazz.isInstance(osBean)) {
                return -1;
            }
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(osBean);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static long invokeLong(String methodName) {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (!clazz.isInstance(osBean)) {
                return -1;
            }
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(osBean);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static long invokeUnixLong(String methodName) {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            Class<?> clazz = Class.forName("com.sun.management.UnixOperatingSystemMXBean");
            if (!clazz.isInstance(osBean)) {
                return -1;
            }
            Method method = clazz.getMethod(methodName);
            Object result = method.invoke(osBean);
            if (result instanceof Number) {
                return ((Number) result).longValue();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
