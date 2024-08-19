package qingzhou.app.instance.monitor;

import qingzhou.api.*;
import qingzhou.api.type.Listable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model(code = "deadlockedthread", icon = "lock", menu = "Monitor",
        name = {"死锁线程", "en:Deadlocked Thread"}, order = 3,
        info = {"汇总死锁的线程，可查看死锁等待对象监视器或同步器的线程栈。",
                "en:To summarize the deadlocked threads, you can view the thread stacks of the deadlock waiting object monitor or synchronizer."})
public class DeadlockedThread extends ModelBase implements Listable {
    @ModelField(list = true,
            name = {"线程ID", "en:Thread Id"},
            info = {"死锁线程的ID。", "en:The ID of the deadlocked thread."})
    public String name;

    @ModelField(
            list = true,
            name = {"线程名", "en:Thread Name"},
            info = {"死锁线程的名称。", "en:The name of the deadlocked thread."})
    public String threadName;

    @ModelField(
            name = {"线程状态", "en:Thread State"},
            list = true, options = {"BLOCKED", "WAITING", "TIMED_WAITING"},
            info = {"死锁线程的当前状态。", "en:The current state of the deadlocked thread."})
    public String threadState;

    @ModelField(list = true,
            name = {"等待的锁", "en:Lock Name"},
            info = {"该死锁线程正在等待的锁名称。", "en:An object for which the thread is blocked waiting."})
    public String lockName;

    @ModelField(
            type = FieldType.number, list = true,
            name = {"锁占有线程ID", "en:Lock Owner Id"},
            info = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的ID。-1 表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The ID of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be -1 if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    public long lockOwnerId;

    @ModelField(list = true,
            name = {"锁占有线程名", "en:Lock Owner Name"},
            info = {"该死锁线程等待的锁正在被其它线程占有，此处给出占有线程的名称。空表示该死锁线程没有在等待锁，或锁没有被其它线程占有。",
                    "en:The name of the thread which owns the object for which the thread associated with this thread is blocked waiting. This will be empty if this thread is not blocked waiting for any object or if the object is not owned by any thread."})
    public String lockOwnerName;

    @ModelField(name = {"死锁线程栈", "en:Deadlocked Thread Stack"},
            info = {"死锁等待对象监视器或同步器的线程栈。", "en:The stack of threads deadlocked waiting for an object monitor or synchronizer."})
    public String deadlockedStack;

    @ModelField(
            type = FieldType.bool,
            name = {"支持强停", "en:Can Be Killed"},
            info = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    public boolean canBeKilled = false;

    @ModelAction(name = {"查看", "en:Show"},
            info = {"查看该死锁线程的信息，包括其死锁的堆栈等。", "en:View information about the deadlocked thread, including its deadlocked stack, etc."})
    public void show(Request request, Response response) throws Exception {
        Map<String, String> data = getDataStore().getDataById(request.getId());
        if (data != null && !data.isEmpty()) {
            response.addData(data);
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
            if (data.get(idFieldName()).equals(tid)) {
                BlockedThread.killThread(Long.parseLong(tid));
                try {
                    Thread.sleep(2000);// 等待线程真正结束，有短暂的存活时间
                } catch (InterruptedException ignored) {
                }
                return;
            }
        }
        getDataStore().deleteDataById(request.getId());
    }
}
