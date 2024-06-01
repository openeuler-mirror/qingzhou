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
        name = {"阻塞线程", "en:Blocked Thread"}, hidden = true,
        info = {"汇总阻塞的线程，可查看阻塞的堆栈及相同堆栈上阻塞的线程。",
                "en:Summarize blocked threads, and view blocked stacks and blocked threads on the same stack."})
public class BlockedThread extends ModelBase implements Listable {

    @ModelField(name = {"线程ID", "en:Thread Id"},
            list = true,
            info = {"阻塞线程的ID。", "en:The ID of the blocked thread."})
    private String id;

    @ModelField(name = {"线程名", "en:Thread Name"},
            list = true,
            info = {"阻塞线程的名称。", "en:The name of the blocked thread."})
    private String threadName;

    @ModelField(name = {"线程状态", "en:Thread State"},
            list = true,
            options = {"BLOCKED", "WAITING", "TIMED_WAITING"},
            info = {"阻塞线程的当前状态。", "en:The current state of the blocked thread."})
    private String threadState;

    @ModelField(name = {"线程栈", "en:Thread Stack"},
            info = {"阻塞线程栈。", "en:Blocking thread stack."})
    private String blockedStack;

    @ModelField(name = {"阻塞方法", "en:Blocked Method"},
            list = true,
            info = {"线程阻塞的类方法。", "en:Thread blocking class method."})
    private String blockedMethod;

    @ModelField(name = {"相同阻塞线程数", "en:Same Blocked Thread Count"},
            list = true,
            info = {"阻塞的线程栈相同的线程数量。", "en:Blocked thread stacks are equal to the number of threads."})
    private Integer sameStackThreadCount;

    @ModelField(name = {"相同阻塞线程", "en:Same Blocked Thread"},
            info = {"阻塞的线程栈相同的线程信息。", "en:Blocked thread stacks the same thread information."})
    private String sameStackThreads;

    @ModelField(
            name = {"支持强停", "en:Can Be Killed"},
            info = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    private boolean canBeKilled = false;

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该阻塞线程的信息，包括其调用的堆栈等。", "en:View the information of the blocking thread, including the stack of its calls."})
    public void show(Request request, Response response) {
        // show 方法已经在 qingzhou.app.ActionMethod.show 中定义，此处的逻辑会被忽略
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
