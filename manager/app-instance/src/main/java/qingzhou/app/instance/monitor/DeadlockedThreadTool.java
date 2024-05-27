package qingzhou.app.instance.monitor;

import qingzhou.engine.util.Utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeadlockedThreadTool {
    private final int MAX_STACK_TRACE_DEPTH = 100;

    public Map<String, String> show(String id) {
        long tid;
        try {
            tid = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }

        boolean found = false;
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null) {
            for (long deadlockedThread : deadlockedThreads) {
                if (deadlockedThread == tid) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return null;
        }

        ThreadInfo threadInfo = threadMXBean.getThreadInfo(tid, MAX_STACK_TRACE_DEPTH);
        if (threadInfo != null) {
            Map<String, String> properties = build(threadInfo);
            // ITAIT-4119
            StackTraceElement[] stackTraceElement = threadInfo.getStackTrace();
            if (stackTraceElement != null) {
                properties.put("deadlockedStack", Utils.stackTraceToString(stackTraceElement));
            }
            return properties;
        } else {
            return null;
        }
    }

    private Map<String, String> build(ThreadInfo threadInfo) {
        Map<String, String> p = new HashMap<>();
        p.put("name", String.valueOf(threadInfo.getThreadId()));
        p.put("threadName", threadInfo.getThreadName());
        p.put("threadState", threadInfo.getThreadState().name());
        p.put("lockName", threadInfo.getLockName());
        p.put("lockOwnerId", String.valueOf(threadInfo.getLockOwnerId()));
        String lockOwnerName = threadInfo.getLockOwnerName();
        if (lockOwnerName != null) {
            p.put("lockOwnerName", lockOwnerName);
        }
        p.put("canBeKilled", String.valueOf(
                threadInfo.getThreadState() != Thread.State.BLOCKED
                        && !threadInfo.getThreadName().equals("main")
        ));
        return p;
    }

    public List<Map<String, String>> list() {
        List<Map<String, String>> list = new ArrayList<>();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null) {
            for (long tid : deadlockedThreads) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(tid, MAX_STACK_TRACE_DEPTH);
                list.add(build(threadInfo));
            }
        }

        return list;
    }
}
