package qingzhou.app.instance.monitor;

import qingzhou.api.DataStore;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Listable;
import qingzhou.deployer.ReadOnlyDataStore;

import java.util.List;
import java.util.Map;

@Model(code = "blockedthread", icon = "pause", menu = "Monitor",
        name = {"阻塞线程", "en:Blocked Thread"}, order = 2,
        info = {"汇总阻塞的线程，可查看阻塞的堆栈及相同堆栈上阻塞的线程。",
                "en:Summarize blocked threads, and view blocked stacks and blocked threads on the same stack."})
public class BlockedThread extends ModelBase implements Listable {

    @ModelField(name = {"线程ID", "en:Thread Id"},
            list = true,
            info = {"阻塞线程的ID。", "en:The ID of the blocked thread."})
    public String id;

    @ModelField(name = {"线程名", "en:Thread Name"},
            list = true,
            info = {"阻塞线程的名称。", "en:The name of the blocked thread."})
    public String threadName;

    @ModelField(name = {"线程状态", "en:Thread State"},
            list = true,
            options = {"BLOCKED", "WAITING", "TIMED_WAITING"},
            info = {"阻塞线程的当前状态。", "en:The current state of the blocked thread."})
    public String threadState;

    @ModelField(name = {"线程栈", "en:Thread Stack"},
            info = {"阻塞线程栈。", "en:Blocking thread stack."})
    public String blockedStack;

    @ModelField(name = {"阻塞方法", "en:Blocked Method"},
            list = true,
            info = {"线程阻塞的类方法。", "en:Thread blocking class method."})
    public String blockedMethod;

    @ModelField(name = {"相同阻塞线程数", "en:Same Blocked Thread Count"},
            list = true,
            info = {"阻塞的线程栈相同的线程数量。", "en:Blocked thread stacks are equal to the number of threads."})
    public Integer sameStackThreadCount;

    @ModelField(name = {"相同阻塞线程", "en:Same Blocked Thread"},
            info = {"阻塞的线程栈相同的线程信息。", "en:Blocked thread stacks the same thread information."})
    public String sameStackThreads;

    @ModelField(
            name = {"支持强停", "en:Can Be Killed"},
            info = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    public boolean canBeKilled = false;

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该阻塞线程的信息，包括其调用的堆栈等。", "en:View the information of the blocking thread, including the stack of its calls."})
    public void show(Request request, Response response) throws Exception {
        List<Map<String, String>> dataList = getDataStore().getDataByIds(new String[]{request.getId()});
        if (!dataList.isEmpty()) {
            response.addData(dataList.get(0));
        }
    }

    @ModelAction(
            show = "canBeKilled=true",
            name = {"强停", "en:Stop"},
            info = {"尝试强制终止该线程的运行。注：该操作可能具有一定的危险，请在确保业务安全的前提下进行。此外，该操作不一定能够成功终止死锁的线程。",
                    "en:Attempts to forcibly terminate the thread may not always succeed for threads in a state such as \"BLOCKED\". Note: This operation may be dangerous, please do it under the premise of ensuring business safety. Also, the operation does not necessarily successfully terminate the deadlocked thread."})
    public void delete(Request request, Response response) throws Exception {
        String tid = request.getId();
        // 确保是自己的线程才能去 kill
        for (Map<String, String> data : getDataStore().getAllData()) {
            if (data.get(Listable.FIELD_NAME_ID).equals(tid)) {
                killThread(Long.parseLong(tid));
                try {
                    Thread.sleep(2000);// 等待线程真正结束，有短暂的存活时间
                } catch (InterruptedException ignored) {
                }
                return;
            }
        }
        getDataStore().deleteDataById(request.getId());
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

    /*
     * 源码来自：com.tongweb.server.loader.WebappClassLoaderBase.getThreads
     */
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

    private final ReadOnlyDataStore dataStore = new ReadOnlyDataStore() {
        private final BlockedThreadTool blockedThreadTool = new BlockedThreadTool();

        @Override
        public List<Map<String, String>> getAllData() {
            return blockedThreadTool.list();
        }

        @Override
        public Map<String, String> getDataById(String id) {
            return blockedThreadTool.show(id);
        }


    };

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }
}
