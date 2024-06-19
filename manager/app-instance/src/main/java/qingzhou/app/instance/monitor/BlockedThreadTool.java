package qingzhou.app.instance.monitor;

import qingzhou.api.type.Listable;
import qingzhou.engine.util.Utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class BlockedThreadTool {

    private static final Set<String> HIDDEN_THREAD = new HashSet<>();
    private static final String[] IGNORED_THREADS_PREFIX = {
            "WebSocketConnectReadThread", "WebSocketWriteThread", // license server连接线程
            "Catalina-", "http-nio-"
    };

    private static final String[] JAVA_STACKS = {"com.sun.", "sun.", "java.", "jdk.", "javax.", "jakarta.", "com.mysql"};
    private static final int MAX_DETECTED_STACK = 20;// 滤掉一些小线程，如nio2的异步等待线程等，通道的默认等待线程探测深度，RMI TCP Connection(2)-* 线程（栈深）

    static {
        HIDDEN_THREAD.add("main");
        HIDDEN_THREAD.add("Reference Handler");
        HIDDEN_THREAD.add("Finalizer");
        HIDDEN_THREAD.add("Single Dispatcher");
        HIDDEN_THREAD.add("Attach Listener");
        HIDDEN_THREAD.add("Common-Cleaner");
        HIDDEN_THREAD.add("Monitor Ctrl-Break");
        HIDDEN_THREAD.add("Notification Thread");
        HIDDEN_THREAD.add("Libgraal MBean Registration");
        HIDDEN_THREAD.add("GC Daemon");
    }

    public Map<String, String> show(String id) {
        long tid;
        try {
            tid = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }

        Map<String, Info> group = group();
        String stack = null;
        Info info = null;
        for (Map.Entry<String, Info> entry : group.entrySet()) {
            ThreadInfo ti = entry.getValue().getThreadInfo();
            if (ti.getThreadId() == tid) {
                stack = entry.getKey();
                info = entry.getValue();
                break;
            }
        }
        if (stack == null) return null;

        Map<String, String> properties = build(info);
        properties.put("blockedStack", stack);

        StringBuilder sb = new StringBuilder();
        for (String name : info.getNames()) {
            sb.append(name).append(System.lineSeparator());
        }
        properties.put("sameStackThreads", sb.toString());

        return properties;
    }

    public List<Map<String, String>> list() {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, Info> group = group();
        for (Map.Entry<String, Info> entry : group.entrySet()) {
            Info value = entry.getValue();
            Map<String, String> properties = build(value);
            list.add(properties);
        }

        list.sort((o1, o2) -> Integer.parseInt(o2.get("sameStackThreadCount")) - Integer.parseInt(o1.get("sameStackThreadCount")));
        return list;
    }

    private Map<String, Info> group() {
        Map<String, Info> map = new HashMap<>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getStackTrace().length <= MAX_DETECTED_STACK) {
                boolean isJavaStackTrace = true;
                for (StackTraceElement traceElement : threadInfo.getStackTrace()) {
                    boolean isJavaStack = false;
                    String className = traceElement.getClassName();
                    for (String javaStack : JAVA_STACKS) {
                        if (className.startsWith(javaStack)) {
                            isJavaStack = true;
                            break;
                        }
                    }
                    if (!isJavaStack) {
                        isJavaStackTrace = false;
                        break;
                    }
                }

                if (isJavaStackTrace) {
                    continue; // NIO2 模式的异步等待线程
                }
            }

            // 运行中的native比较慢，应该被认定为阻塞线程，运行中且非native的是非阻塞线程
            if (threadInfo.getThreadState() == Thread.State.RUNNABLE && !threadInfo.isInNative()) {
                continue;
            }

            if (isIgnoredThreadInfo(threadInfo)) {
                continue;
            }

            String stackTrace = Utils.stackTraceToString(threadInfo.getStackTrace());
            if (stackTrace.isEmpty()) continue;

            Info infos = map.computeIfAbsent(stackTrace, s -> new Info(threadInfo));
            infos.getCount().incrementAndGet();
            infos.getNames().add(threadInfo.getThreadName());
        }
        return map;
    }

    private boolean isIgnoredThreadInfo(ThreadInfo threadInfo) {
        if (HIDDEN_THREAD.contains(threadInfo.getThreadName())) {
            return true;
        }

        for (String threadPrefix : IGNORED_THREADS_PREFIX) {
            if (threadInfo.getThreadName().startsWith(threadPrefix)) {
                return true;
            }
        }

        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        if (stackTrace == null) return true;


        OUT:
        for (int i = 0; i < stackTrace.length; i++) {
            if (i >= MAX_DETECTED_STACK) break; // 最多探测的栈深为8，多数场景可以覆盖到了

            String className = stackTrace[i].getClassName();

            for (String javaStack : JAVA_STACKS) {
                if (className.startsWith(javaStack)) {
                    continue OUT;
                }
            }

            break;
        }

        return false;
    }

    private Map<String, String> build(Info info) {
        Map<String, String> p = new HashMap<>();
        ThreadInfo threadInfo = info.getThreadInfo();
        p.put(Listable.FIELD_NAME_ID, String.valueOf(threadInfo.getThreadId()));
        p.put("threadName", threadInfo.getThreadName());
        p.put("threadState", threadInfo.getThreadState().name());
        p.put("blockedMethod", threadInfo.getStackTrace()[0].toString());
        p.put("sameStackThreadCount", String.valueOf(info.getCount()));
        p.put("canBeKilled", String.valueOf(
                threadInfo.getThreadState() != Thread.State.BLOCKED
                        && !threadInfo.getThreadName().equals("main")
        ));
        return p;
    }

    private static class Info {
        private final ThreadInfo threadInfo;
        private final AtomicInteger count = new AtomicInteger();
        private final List<String> names = new ArrayList<>();

        private Info(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        public ThreadInfo getThreadInfo() {
            return threadInfo;
        }

        public AtomicInteger getCount() {
            return count;
        }

        public List<String> getNames() {
            return names;
        }
    }
}
