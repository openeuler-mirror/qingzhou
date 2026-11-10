package qingzhou.app.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * 通过本地 Attach 连接 Tomcat 进程的 MBeanServer，读取 Connector、应用、线程池等运行时信息。
 * JDK 8 的 attach API 只存在于 tools.jar 且不在启动 classpath 中，故用独立类加载器按需加载。
 */
final class TomcatJmx {
    private static final String BOOTSTRAP = "org.apache.catalina.startup.Bootstrap";
    private static TomcatJmx shared;

    private final JMXConnector connector;
    private final MBeanServerConnection mBeanServer;
    private String pid;

    static synchronized TomcatJmx get(String tomcatHome) throws Exception {
        if (shared == null) {
            shared = new TomcatJmx(tomcatHome);
        }
        return shared;
    }

    /**
     * 连接失效（如 Tomcat 重启）时丢弃缓存，下次调用重新 attach。
     */
    private static synchronized void invalidate() {
        if (shared == null) {
            return;
        }
        try {
            shared.connector.close();
        } catch (Exception e) {
            // 关闭失败不影响重连
        }
        shared = null;
    }

    interface Reader<T> {
        T read(TomcatJmx jmx) throws Exception;
    }

    /**
     * Tomcat 重启会让缓存的 JMX 连接失效，此处丢弃旧连接并整体重试一次，避免重启后首次访问报错。
     */
    static <T> T read(String tomcatHome, Reader<T> reader) throws Exception {
        try {
            return reader.read(get(tomcatHome));
        } catch (Exception e) {
            invalidate();
            return reader.read(get(tomcatHome));
        }
    }

    private TomcatJmx(String tomcatHome) throws Exception {
        Class<?> vmClass = attachClass();
        Object vm = attachTomcat(vmClass, tomcatHome);
        try {
            String address = (String) vmClass.getMethod("startLocalManagementAgent").invoke(vm);
            connector = JMXConnectorFactory.connect(new JMXServiceURL(address), null);
            mBeanServer = connector.getMBeanServerConnection();
        } finally {
            vmClass.getMethod("detach").invoke(vm);
        }
    }

    List<Map<String, String>> connectors() throws Exception {
        return query("Catalina:type=Connector,*", name -> {
            Map<String, String> connector = new LinkedHashMap<>();
            connector.put("id", attribute(name, "port"));
            connector.put("protocol", attribute(name, "protocol"));
            connector.put("port", attribute(name, "port"));
            connector.put("address", attribute(name, "address"));
            connector.put("maxThreads", attribute(name, "maxThreads"));
            connector.put("minSpareThreads", attribute(name, "minSpareThreads"));
            connector.put("acceptCount", attribute(name, "acceptCount"));
            connector.put("connectionTimeout", attribute(name, "connectionTimeout"));
            connector.put("redirectPort", attribute(name, "redirectPort"));
            return connector;
        });
    }

    List<Map<String, String>> applications() throws Exception {
        return query("Catalina:j2eeType=WebModule,*", name -> {
            String contextPath = attribute(name, "path");
            Map<String, String> app = new LinkedHashMap<>();
            app.put("name", TomcatModelBase.appName(contextPath));
            app.put("contextPath", contextPath);
            app.put("docBase", attribute(name, "docBase"));
            app.put("reloadable", attribute(name, "reloadable"));
            app.put("status", attribute(name, "stateName"));
            return app;
        });
    }

    List<Map<String, String>> threadPools() throws Exception {
        return query("Catalina:type=ThreadPool,*", name -> {
            Map<String, String> pool = new LinkedHashMap<>();
            pool.put("name", ObjectName.unquote(name.getKeyProperty("name")));
            for (String field : THREAD_POOL_FIELDS) {
                pool.put(field, attribute(name, field));
            }
            return pool;
        });
    }

    // 与 TomcatThreadPool 的模型字段一一对应，取自 ThreadPool MBean 实际暴露的属性
    private static final String[] THREAD_POOL_FIELDS = {
            "port", "localPort", "maxThreads", "minSpareThreads",
            "currentThreadCount", "currentThreadsBusy", "connectionCount", "keepAliveCount",
            "maxConnections", "backlog", "acceptorThreadCount", "pollerThreadCount",
            "keepAliveTimeout", "maxKeepAliveRequests", "soTimeout", "running", "paused"};

    /**
     * 首页汇总：Server / Connector / WebModule MBean 的基础信息与数量。
     */
    Map<String, String> stats() throws Exception {
        try {
            Map<String, String> stats = new LinkedHashMap<>();
            stats.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            Set<ObjectName> servers = names("Catalina:type=Server,*");
            if (!servers.isEmpty()) {
                stats.put("serverInfo", attribute(servers.iterator().next(), "serverInfo"));
            }
            stats.put("connectorCount", String.valueOf(names("Catalina:type=Connector,*").size()));
            stats.put("applicationCount", String.valueOf(names("Catalina:j2eeType=WebModule,*").size()));
            return stats;
        } catch (Exception e) {
            invalidate();
            throw e;
        }
    }

    /**
     * JVM 级指标（目标 Tomcat 进程的 java.lang 域），供 jvm 模型使用。
     */
    Map<String, String> jvmStats() throws Exception {
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        addJvmStats(stats);
        return stats;
    }

    /**
     * 系统级指标（目标进程与操作系统资源），供 system 模型使用。
     */
    Map<String, String> systemStats() throws Exception {
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        addProcessStats(stats);
        return stats;
    }

    /**
     * JVM 指标取自目标 JVM 自带的 java.lang MBean（内存、CPU、线程、GC、运行时长）。
     */
    private void addJvmStats(Map<String, String> stats) throws Exception {
        ObjectName memory = new ObjectName("java.lang:type=Memory");
        long heapUsed = compositeNumber(memory, "HeapMemoryUsage", "used");
        long heapMax = compositeNumber(memory, "HeapMemoryUsage", "max");
        if (heapMax <= 0) { // MBean 契约：max 为 -1 表示上限未定义，退化为已提交内存
            heapMax = compositeNumber(memory, "HeapMemoryUsage", "committed");
        }
        stats.put("heapUsedMb", mb(heapUsed));
        stats.put("heapMaxMb", mb(heapMax));
        // 保留两位小数：堆占用常低于 1%，取整会恒显 0，仪表图无法反映真实水位
        stats.put("heapUsedPercent", heapMax == 0 ? "0" : String.format("%.2f", heapUsed * 100.0 / heapMax));
        stats.put("nonHeapUsedMb", mb(compositeNumber(memory, "NonHeapMemoryUsage", "used")));

        ObjectName os = new ObjectName("java.lang:type=OperatingSystem");
        stats.put("processCpuPercent", percent(os, "ProcessCpuLoad"));

        ObjectName threading = new ObjectName("java.lang:type=Threading");
        stats.put("jvmThreadCount", String.valueOf(number(threading, "ThreadCount")));

        long gcCount = 0, gcTime = 0;
        for (ObjectName name : names("java.lang:type=GarbageCollector,*")) {
            gcCount += number(name, "CollectionCount");
            gcTime += number(name, "CollectionTime");
        }
        stats.put("gcCount", String.valueOf(gcCount));
        stats.put("gcTimeMs", String.valueOf(gcTime));

        stats.put("loadedClassCount", String.valueOf(number(new ObjectName("java.lang:type=ClassLoading"), "LoadedClassCount")));
        stats.put("uptimeHours", String.format("%.1f",
                number(new ObjectName("java.lang:type=Runtime"), "Uptime") / 3600000.0));
    }

    /**
     * 进程级指标：PID、启动时间、常驻内存、文件句柄与 JVM 参数，便于排查进程层面问题。
     */
    private void addProcessStats(Map<String, String> stats) throws Exception {
        stats.put("pid", pid);
        ObjectName runtime = new ObjectName("java.lang:type=Runtime");
        stats.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(number(runtime, "StartTime"))));
        stats.put("jvmName", attribute(runtime, "VmName") + " " + attribute(runtime, "VmVersion"));
        stats.put("vmArgs", String.join(" ", (String[]) mBeanServer.getAttribute(runtime, "InputArguments")));

        ObjectName os = new ObjectName("java.lang:type=OperatingSystem");
        long rssKb = rssKb();
        long totalBytes = number(os, "TotalPhysicalMemorySize");
        stats.put("rssMb", String.format("%.2f", rssKb / 1024.0));
        stats.put("rssPercent", totalBytes == 0 ? "0" : String.format("%.2f", rssKb * 1024.0 * 100 / totalBytes));
        stats.put("systemCpuPercent", percent(os, "SystemCpuLoad"));
        stats.put("physicalTotalMb", mb(totalBytes));
        stats.put("physicalFreeMb", mb(number(os, "FreePhysicalMemorySize")));
        stats.put("openFileCount", String.valueOf(number(os, "OpenFileDescriptorCount")));
        stats.put("maxFileCount", String.valueOf(number(os, "MaxFileDescriptorCount")));
    }

    /**
     * 常驻内存 JDK 未暴露，按操作系统 ps 读取；Windows 等无 ps 的平台返回 0（契约适配）。
     */
    private long rssKb() {
        try {
            Process process = new ProcessBuilder("ps", "-o", "rss=", "-p", pid).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                return line == null || line.trim().isEmpty() ? 0 : Long.parseLong(line.trim());
            } finally {
                process.destroy();
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private long compositeNumber(ObjectName name, String attribute, String item) throws Exception {
        Object value = mBeanServer.getAttribute(name, attribute);
        Object item0 = value instanceof CompositeData ? ((CompositeData) value).get(item) : null;
        return item0 instanceof Number ? ((Number) item0).longValue() : 0;
    }

    /**
     * JVM CPU 负载返回 0.0~1.0，-1 表示当前平台不可用。
     * 保留两位小数：进程空闲时负载常低于 1%，取整会恒为 0，无法反映真实占用。
     */
    private String percent(ObjectName name, String attribute) throws Exception {
        Object value = mBeanServer.getAttribute(name, attribute);
        double load = value instanceof Number ? ((Number) value).doubleValue() : -1;
        return load < 0 ? "-1" : String.format("%.2f", load * 100);
    }

    /**
     * 单个 Connector 的监控项：线程取自同端口 ThreadPool，流量与耗时取自同名 GlobalRequestProcessor。
     */
    Map<String, String> connectorStats(String port) throws Exception {
        try {
            long threads = 0, busy = 0, maxThreads = 0;
            String handler = null;
            for (ObjectName name : names("Catalina:type=ThreadPool,*")) {
                if (!port.equals(attribute(name, "port"))) {
                    continue;
                }
                handler = ObjectName.unquote(name.getKeyProperty("name"));
                threads = number(name, "currentThreadCount");
                busy = number(name, "currentThreadsBusy");
                maxThreads = number(name, "maxThreads");
                break;
            }

            long requests = 0, errors = 0, sent = 0, received = 0, processing = 0, maxTime = 0;
            ObjectName processor = handler == null ? null
                    : new ObjectName("Catalina:type=GlobalRequestProcessor,name=" + ObjectName.quote(handler));
            if (processor != null && mBeanServer.isRegistered(processor)) {
                requests = number(processor, "requestCount");
                errors = number(processor, "errorCount");
                sent = number(processor, "bytesSent");
                received = number(processor, "bytesReceived");
                processing = number(processor, "processingTime");
                maxTime = number(processor, "maxTime");
            }

            Map<String, String> stats = new LinkedHashMap<>();
            stats.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            stats.put("requestCount", String.valueOf(requests));
            stats.put("errorCount", String.valueOf(errors));
            stats.put("bytesSent", mb(sent));
            stats.put("bytesReceived", mb(received));
            stats.put("threadBusyPercent", String.valueOf(maxThreads == 0 ? 0 : busy * 100 / maxThreads));
            stats.put("threadBusy", String.valueOf(busy));
            stats.put("threadIdle", String.valueOf(threads - busy));
            stats.put("avgProcessingTime", String.valueOf(requests == 0 ? 0 : processing / requests));
            stats.put("maxProcessingTime", String.valueOf(maxTime));
            return stats;
        } catch (Exception e) {
            invalidate();
            throw e;
        }
    }

    /**
     * 线程池监视：线程容量与连接占用，取自 ThreadPool MBean。
     */
    Map<String, String> threadPoolStats(String poolName) throws Exception {
        try {
            ObjectName target = null;
            for (ObjectName name : names("Catalina:type=ThreadPool,*")) {
                if (poolName.equals(ObjectName.unquote(name.getKeyProperty("name")))) {
                    target = name;
                    break;
                }
            }
            if (target == null) {
                throw new IllegalStateException("运行中的 Tomcat 未找到线程池: " + poolName);
            }
            long threads = number(target, "currentThreadCount");
            long busy = number(target, "currentThreadsBusy");
            long maxThreads = number(target, "maxThreads");
            Map<String, String> stats = new LinkedHashMap<>();
            stats.put("statsTime", now());
            stats.put("threadCount", String.valueOf(threads));
            stats.put("busyThreads", String.valueOf(busy));
            stats.put("idleThreads", String.valueOf(threads - busy));
            stats.put("busyPercent", String.valueOf(maxThreads == 0 ? 0 : busy * 100 / maxThreads));
            stats.put("liveConnections", attribute(target, "connectionCount"));
            stats.put("keepAliveConnections", attribute(target, "keepAliveCount"));
            return stats;
        } catch (Exception e) {
            invalidate();
            throw e;
        }
    }

    /**
     * 应用监视：请求量、耗时与会话，取自 WebModule 与同上下文 Manager MBean。
     */
    Map<String, String> applicationStats(String appName) throws Exception {
        try {
            for (ObjectName name : names("Catalina:j2eeType=WebModule,*")) {
                String contextPath = attribute(name, "path");
                if (!TomcatModelBase.appName(contextPath).equals(appName)) {
                    continue;
                }
                long requests = number(name, "requestCount");
                Map<String, String> stats = new LinkedHashMap<>();
                stats.put("statsTime", now());
                stats.put("requestCount", String.valueOf(requests));
                stats.put("errorCount", String.valueOf(number(name, "errorCount")));
                stats.put("avgProcessingTime", String.valueOf(requests == 0 ? 0 : number(name, "processingTime") / requests));
                stats.put("maxProcessingTime", String.valueOf(number(name, "maxTime")));
                // Tomcat 无请求时 minTime 为 Long.MAX_VALUE，不能直接展示
                stats.put("minProcessingTime", String.valueOf(requests == 0 ? 0 : number(name, "minTime")));
                stats.put("activeSessions", String.valueOf(activeSessions(contextPath)));
                stats.put("stateName", attribute(name, "stateName"));
                return stats;
            }
            throw new IllegalStateException("运行中的 Tomcat 未找到应用: " + appName);
        } catch (Exception e) {
            invalidate();
            throw e;
        }
    }

    private long activeSessions(String contextPath) throws Exception {
        long sessions = 0;
        for (ObjectName name : names("Catalina:type=Manager,*")) {
            if (contextPath.equals(attribute(name, "path"))) {
                sessions += number(name, "activeSessions");
            }
        }
        return sessions;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private Set<ObjectName> names(String pattern) throws Exception {
        return mBeanServer.queryNames(new ObjectName(pattern), null);
    }

    private long number(ObjectName name, String attribute) {
        String value = attribute(name, attribute);
        return value.isEmpty() ? 0 : Long.parseLong(value);
    }

    private static String mb(long bytes) {
        return String.format("%.2f", bytes / 1024.0 / 1024.0);
    }

    private List<Map<String, String>> query(String pattern, RowBuilder builder) throws Exception {
        try {
            List<Map<String, String>> result = new ArrayList<>();
            for (ObjectName name : mBeanServer.queryNames(new ObjectName(pattern), null)) {
                result.add(builder.build(name));
            }
            return result;
        } catch (Exception e) {
            invalidate();
            throw e;
        }
    }

    private interface RowBuilder {
        Map<String, String> build(ObjectName name);
    }

    /**
     * Tomcat 各版本暴露的 MBean 属性集合不同，缺失属性按空值展示。
     */
    private String attribute(ObjectName name, String attribute) {
        try {
            Object value = mBeanServer.getAttribute(name, attribute);
            if (value == null) {
                return "";
            }
            // Connector 的 address 属性是 InetAddress，直接 toString 会带 "/" 前缀
            return value instanceof InetAddress ? ((InetAddress) value).getHostAddress() : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private static Class<?> attachClass;

    private static synchronized Class<?> attachClass() throws Exception {
        if (attachClass != null) {
            return attachClass;
        }
        File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (toolsJar.isFile()) {
            // 多个 Tomcat 实例（bundle :0/:1）各有独立类加载器，若各自新建 URLClassLoader 加载 tools.jar，
            // 原生库 libattach 会在第二个实例加载时抛出 already loaded in another classloader。
            // 须把 tools.jar 注入系统类加载器，所有实例共享同一个 VirtualMachine 类与 libattach。
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            java.lang.reflect.Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(systemLoader, toolsJar.toURI().toURL());
            attachClass = Class.forName("com.sun.tools.attach.VirtualMachine", true, systemLoader);
        } else {
            // JDK 9+ 的 attach 类在 jdk.attach 模块内，Bundle 类加载器不可见，须由系统类加载器加载
            attachClass = Class.forName("com.sun.tools.attach.VirtualMachine", true, ClassLoader.getSystemClassLoader());
        }
        return attachClass;
    }

    /**
     * 多个 Tomcat 进程并存时，以 catalina.home 与探测到的安装路径匹配，避免连错实例。
     */
    private Object attachTomcat(Class<?> vmClass, String tomcatHome) throws Exception {
        List<?> descriptors = (List<?>) vmClass.getMethod("list").invoke(null);
        for (Object descriptor : descriptors) {
            String displayName = (String) descriptor.getClass().getMethod("displayName").invoke(descriptor);
            if (displayName == null || !displayName.contains(BOOTSTRAP)) {
                continue;
            }
            String targetPid = (String) descriptor.getClass().getMethod("id").invoke(descriptor);
            Object vm = null;
            boolean matched = false;
            try {
                vm = vmClass.getMethod("attach", String.class).invoke(null, targetPid);
                Properties props = (Properties) vmClass.getMethod("getSystemProperties").invoke(vm);
                matched = samePath(tomcatHome, props.getProperty("catalina.home"));
                if (matched) {
                    pid = targetPid;
                    return vm;
                }
            } catch (Exception e) {
                // 单个进程 attach 或读属性失败（权限不足、进程已退出）不应中断对其余候选的匹配
            } finally {
                if (!matched && vm != null) {
                    try {
                        vmClass.getMethod("detach").invoke(vm);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        throw new IllegalStateException("未找到与安装路径匹配的运行中 Tomcat: " + tomcatHome);
    }

    private static boolean samePath(String left, String right) {
        try {
            return new File(left).getCanonicalFile().equals(new File(right).getCanonicalFile());
        } catch (Exception e) {
            return left.equals(right);
        }
    }
}
