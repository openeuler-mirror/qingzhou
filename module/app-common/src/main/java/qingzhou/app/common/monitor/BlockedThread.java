package qingzhou.app.common.monitor;


import qingzhou.api.*;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Showable;
import qingzhou.framework.app.ReadOnlyDataStore;

import java.util.List;
import java.util.Map;

/**
 * @author huang lei
 * @date 2022/9/1 15:51
 */
@Model(name = "blockedthread", icon = "pause",
        nameI18n = {"阻塞线程", "en:Blocked Thread"},
        infoI18n = {"汇总阻塞的线程，可查看阻塞的堆栈及相同堆栈上阻塞的线程。",
                "en:Summarize blocked threads, and view blocked stacks and blocked threads on the same stack."})
public class BlockedThread extends ModelBase implements Listable {

    @ModelField(nameI18n = {"线程ID", "en:Thread Id"},
            showToList = true,
            infoI18n = {"阻塞线程的ID。", "en:The ID of the blocked thread."})
    private String id;

    @ModelField(nameI18n = {"线程名", "en:Thread Name"},
            showToList = true,
            infoI18n = {"阻塞线程的名称。", "en:The name of the blocked thread."})
    private String threadName;

    @ModelField(nameI18n = {"线程状态", "en:Thread State"},
            type = FieldType.select,
            showToList = true,
            infoI18n = {"阻塞线程的当前状态。", "en:The current state of the blocked thread."})
    private String threadState;

    @ModelField(nameI18n = {"线程栈", "en:Thread Stack"},
            infoI18n = {"阻塞线程栈。", "en:Blocking thread stack."})
    private String blockedStack;

    @ModelField(nameI18n = {"阻塞方法", "en:Blocked Method"},
            showToList = true,
            infoI18n = {"线程阻塞的类方法。", "en:Thread blocking class method."})
    private String blockedMethod;

    @ModelField(nameI18n = {"相同阻塞线程数", "en:Same Blocked Thread Count"},
            showToList = true,
            infoI18n = {"阻塞的线程栈相同的线程数量。", "en:Blocked thread stacks are equal to the number of threads."})
    private Integer sameStackThreadCount;

    @ModelField(nameI18n = {"相同阻塞线程", "en:Same Blocked Thread"},
            infoI18n = {"阻塞的线程栈相同的线程信息。", "en:Blocked thread stacks the same thread information."})
    private String sameStackThreads;

    @ModelField(
            nameI18n = {"支持强停", "en:Can Be Killed"},
            infoI18n = {"标记该线程是否支持强制终止。",
                    "en:Marks whether the thread supports forced termination."})
    private boolean canBeKilled = false;

    @ModelAction(name = Showable.ACTION_NAME_SHOW, nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该阻塞线程的信息，包括其调用的堆栈等。", "en:View the information of the blocking thread, including the stack of its calls."})
    public void show(Request request, Response response) throws Exception {
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
        private final BlockedThreadTool blockedThreadTool = new BlockedThreadTool();

        @Override
        public List<Map<String, String>> getAllData(String type) {
            return blockedThreadTool.list();
        }

        @Override
        public Map<String, String> getDataById(String type, String id) {
            return blockedThreadTool.show(id);
        }
    };

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }
}
