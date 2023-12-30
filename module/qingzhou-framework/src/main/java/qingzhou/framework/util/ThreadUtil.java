package qingzhou.framework.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadUtil {
    private static final Pattern STACK_PATTERN = Pattern.compile("\"(?<threadName>.*)\"( #(?<threadId>\\d+))?.*( cpu=(?<cpu>\\d+\\.\\d+)ms)? .* nid=0x(?<nid>\\w+) .*");

    public static ExecutorService newExecutorService(String name, int corePoolSize) {
        return Executors.newFixedThreadPool(corePoolSize, new ThreadFactory(name));
    }

    public static ScheduledExecutorService newScheduledThreadPool(String name, int corePoolSize) {
        return Executors.newScheduledThreadPool(corePoolSize, new ThreadFactory(name));
    }

    public static void killThread(long id) {
        if (id < 1) return;
        Thread[] threads = getThreads();
        Thread thread = null;
        for (Thread t : threads) {
            if (t != null && t.getId() == id) {
                thread = t;
                break;
            }
        }
        if (thread != null) {
            killThread(thread);
        }
    }

    public static void killThread(Thread thread) {
        if (thread == null) return;
        try {
            thread.interrupt();
        } catch (Throwable ignored) {
        }
        try {
            thread.stop();
        } catch (Throwable ignored) {
        }
    }

    public static Thread[] getThreads() {
        // Get the current thread group
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        // Find the root thread group
        try {
            while (tg.getParent() != null) {
                tg = tg.getParent();
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        int threadCountGuess = tg.activeCount() + 50;
        Thread[] threads = new Thread[threadCountGuess];
        int threadCountActual = tg.enumerate(threads);
        // Make sure we don't miss any threads
        while (threadCountActual == threadCountGuess) {
            threadCountGuess *= 2;
            threads = new Thread[threadCountGuess];
            // Note tg.enumerate(Thread[]) silently ignores any threads that
            // can't fit into the array
            threadCountActual = tg.enumerate(threads);
        }

        return threads;
    }

    public static String stackTraceString(StackTraceElement[] stackTrace) {
        StringBuilder msg = new StringBuilder();
        String sp = System.lineSeparator();
        for (StackTraceElement element : stackTrace) {
            msg.append("\t").append(element).append(sp);
        }
        return msg.toString();
    }

    public static List<Map<String, String>> getBusyThreadInfo(String pid) throws Exception {
        Map<String, Double> threadCpuLoad = getThreadCpuLoad(pid);
        if (threadCpuLoad != null) {
            String jstack = JDKUtil.getJavaBinTool("jstack");
            if (StringUtil.notBlank(jstack)) {
                File stackFile = File.createTempFile("jstack-" + pid, null, null);
                try {
                    String command = jstack + " " + pid + " > " + stackFile.getCanonicalPath();
                    int result = NativeCommandUtil.runNativeCommand(command, null, null);
                    if (result == 0) {
                        Set<String> nids = threadCpuLoad.keySet();
                        List<Map<String, String>> list = new ArrayList<>();
                        for (Map<String, String> map : parseResult(stackFile)) {
                            if (nids.contains(map.get("nid1"))) {
                                String nid1 = map.get("nid1");
                                map.put("nid", nid1 + "/0x" + map.get("nid0"));
                                map.put("cpuLoad", threadCpuLoad.get(nid1).toString());
                                list.add(map);
                            }
                        }
                        Comparator<Map<String, String>> comparing = Comparator.comparing(p -> Double.parseDouble(p.get("cpuLoad")));
                        list.sort(comparing.reversed());
                        return list;
                    }
                } finally {
                    FileUtil.forceDeleteQuietly(stackFile);
                }
            } else {
                System.out.println("The jstack tool does not exist");
            }
        }
        return null;
    }

    private static List<Map<String, String>> parseResult(File stackFile) throws IOException {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = null;
        for (String line : Files.readAllLines(stackFile.toPath())) {
            Matcher matcher = STACK_PATTERN.matcher(line);
            if (matcher.find()) {
                map = new HashMap<>();
                map.put("name", matcher.group("threadName"));
                String threadId = matcher.group("threadId");
                if (threadId != null) {
                    map.put("threadId", threadId);
                }
                String nid0 = matcher.group("nid"); // 16进制
                map.put("nid0", nid0);
                map.put("nid1", String.valueOf(Integer.parseInt(nid0, 16))); // 转10进制
                String cpu = matcher.group("cpu");
                if (cpu != null) {
                    map.put("cpuTime", cpu);
                }
                list.add(map);
            }
            if (StringUtil.notBlank(line) && map != null) {
                String stack = map.getOrDefault("stack", "");
                map.put("stack", stack + line + System.lineSeparator());
            }
            if (StringUtil.isBlank(line)) {
                map = null;
            }
        }

        return list;
    }

    private static Map<String, Double> getThreadCpuLoad(String pid) throws Exception {
        if (OSUtil.IS_LINUX) {
            File file = File.createTempFile("ps-" + pid + "-", null);
            try {
                int result = NativeCommandUtil.runNativeCommand(String.format("ps -p %s -wwLo lwp,pcpu --no-headers > %s", pid, file.getCanonicalPath()), null, null);
                if (result == 0) {
                    Map<String, Double> map = new HashMap<>();
                    for (String line : Files.readAllLines(file.toPath())) {
                        String[] s = line.trim().split(" +");
                        map.put(s[0], Double.valueOf(s[1]));
                    }
                    FileUtil.forceDelete(file);
                    return map;
                }
            } finally {
                FileUtil.forceDeleteQuietly(file);
            }
        }
        return null;
    }

    private ThreadUtil() {
    }

    public static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String threadFlag;

        public ThreadFactory(String threadFlag) {
            this.threadFlag = threadFlag;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, Constants.QZ_ + threadFlag + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
