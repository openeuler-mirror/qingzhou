package qingzhou.app.common.monitor;

import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Showable;
import qingzhou.framework.app.ReadOnlyDataStore;

import java.util.List;
import java.util.Map;

@Model(name = "deadlockedthread", icon = "lock",
        nameI18n = {"死锁线程", "en:Deadlocked Thread"},
        infoI18n = {"汇总死锁的线程，可查看死锁等待对象监视器或同步器的线程栈。",
                "en:To summarize the deadlocked threads, you can view the thread stacks of the deadlock waiting object monitor or synchronizer."})
public class DeadlockedThread extends ModelBase implements Listable {
    @ModelField(nameI18n = {"线程ID", "en:Thread Id"},
            showToList = true,
            infoI18n = {"死锁线程的ID。", "en:The ID of the deadlocked thread."})
    private String name;

    @ModelField(nameI18n = {"线程名", "en:Thread Name"},
            showToList = true,
            infoI18n = {"死锁线程的名称。", "en:The name of the deadlocked thread."})
    private String threadName;

    @ModelField(nameI18n = {"线程状态", "en:Thread State"},
            type = FieldType.select,
            showToList = true,
            infoI18n = {"死锁线程的当前状态。", "en:The current state of the deadlocked thread."})
    private String threadState;

    @ModelField(nameI18n = {"等待的锁", "en:Lock Name"},
            showToList = true,
            infoI18n = {"该死锁线程正在等待的锁名称。",
                    "en:An object for which the thread is blocked waiting."})
    private String lockName;

    @ModelField(nameI18n = {"锁占有线程ID", "en:Lock Owner Id"},
            showToList = true,
            infoI18n = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的ID。-1 表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The ID of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be -1 if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    private long lockOwnerId;

    @ModelField(nameI18n = {"锁占有线程名", "en:Lock Owner Name"},
            showToList = true,
            infoI18n = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的名称。空表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The name of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be empty if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    private String lockOwnerName;

    @ModelField(nameI18n = {"死锁线程栈", "en:Deadlocked Thread Stack"},
            infoI18n = {"死锁等待对象监视器或同步器的线程栈。", "en:The stack of threads deadlocked waiting for an object monitor or synchronizer."})
    private String deadlockedStack;

    @ModelField(
            nameI18n = {"支持强停", "en:Can Be Killed"},
            infoI18n = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    private boolean canBeKilled = false;

    @ModelAction(name = Showable.ACTION_NAME_SHOW, nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该死锁线程的信息，包括其死锁的堆栈等。", "en:View information about the deadlocked thread, including its deadlocked stack, etc."})
    public void show(Request request, Response response) {
        // show 方法已经在 qingzhou.app.ActionMethod.show 中定义，此处的逻辑会被忽略
    }

    @Override
    public Options options(Request request, String fieldName) {
        if ("threadState".equals(fieldName)) {
            return Options.of("BLOCKED", "WAITING", "TIMED_WAITING");
        }

        return super.options(request, fieldName);
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
