package qingzhou.app.common.monitor;

import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Showable;
import qingzhou.deployer.ReadOnlyDataStore;

import java.util.List;
import java.util.Map;

@Model(code = "deadlockedthread", icon = "lock",
        name = {"死锁线程", "en:Deadlocked Thread"},
        info = {"汇总死锁的线程，可查看死锁等待对象监视器或同步器的线程栈。",
                "en:To summarize the deadlocked threads, you can view the thread stacks of the deadlock waiting object monitor or synchronizer."})
public class DeadlockedThread extends ModelBase implements Listable {
    @ModelField(name = {"线程ID", "en:Thread Id"},
            list = true,
            info = {"死锁线程的ID。", "en:The ID of the deadlocked thread."})
    private String name;

    @ModelField(name = {"线程名", "en:Thread Name"},
            list = true,
            info = {"死锁线程的名称。", "en:The name of the deadlocked thread."})
    private String threadName;

    @ModelField(
            name = {"线程状态", "en:Thread State"},
            list = true, options = {"BLOCKED", "WAITING", "TIMED_WAITING"},
            info = {"死锁线程的当前状态。", "en:The current state of the deadlocked thread."})
    private String threadState;

    @ModelField(name = {"等待的锁", "en:Lock Name"},
            list = true,
            info = {"该死锁线程正在等待的锁名称。",
                    "en:An object for which the thread is blocked waiting."})
    private String lockName;

    @ModelField(name = {"锁占有线程ID", "en:Lock Owner Id"},
            list = true,
            info = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的ID。-1 表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The ID of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be -1 if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    private long lockOwnerId;

    @ModelField(name = {"锁占有线程名", "en:Lock Owner Name"},
            list = true,
            info = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的名称。空表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The name of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be empty if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    private String lockOwnerName;

    @ModelField(name = {"死锁线程栈", "en:Deadlocked Thread Stack"},
            info = {"死锁等待对象监视器或同步器的线程栈。", "en:The stack of threads deadlocked waiting for an object monitor or synchronizer."})
    private String deadlockedStack;

    @ModelField(
            name = {"支持强停", "en:Can Be Killed"},
            info = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    private boolean canBeKilled = false;

    @ModelAction(name = Showable.ACTION_NAME_SHOW, name = {"查看", "en:Show"},
            info = {"查看该死锁线程的信息，包括其死锁的堆栈等。", "en:View information about the deadlocked thread, including its deadlocked stack, etc."})
    public void show(Request request, Response response) {
        // show 方法已经在 qingzhou.app.ActionMethod.show 中定义，此处的逻辑会被忽略
    }

    private final ReadOnlyDataStore dataStore = new ReadOnlyDataStore() {
        private final DeadlockedThreadTool tool = new DeadlockedThreadTool();

        @Override
        public List<Map<String, String>> getAllData(String type) {
            return tool.list();
        }

        @Override
        public Map<String, String> getDataById(String type, String id) {
            return tool.show(id);
        }
    };

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }
}
