package qingzhou.app.demo;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Page;

/**
 * 无操作列表：仅实现 {@link Page}（不实现 Show/Add/Update/Delete），
 * 用于演示「纯列表页」——无行内操作列、仅分页与搜索。
 */
@Model(code = "simple-list", order = 12,
        name = {"无操作列表", "en:No-Op List"},
        info = {"演示纯列表页（无行操作），25条数据验证分页和无操作列", "en:Demo list-only page with pagination, no row actions"},
        icon = "List",
        menu = "advanced")
public class SimpleListDemo extends AbstractPageModel implements Page {
    private static final Map<String, Map<String, String>> db = new HashMap<>();

    public SimpleListDemo() {
        if (!db.isEmpty()) return;

        String[] names = {"张三", "李四", "王五", "赵六", "孙七", "周八", "吴九", "郑十", "冯十一", "陈十二"};
        String[] categories = {"已完成", "进行中", "待开始", "已取消", "已延期"};
        String[] priorities = {"高", "中", "低"};

        for (int i = 1; i <= 25; i++) {
            Map<String, String> t = new HashMap<>();
            t.put("id", "T" + String.format("%03d", i));
            t.put("title", "任务" + i + "：完成" + names[i % names.length] + "相关模块开发");
            t.put("assignee", names[i % names.length]);
            t.put("category", categories[i % categories.length]);
            t.put("priority", priorities[i % priorities.length]);
            t.put("progress", (i * 17) % 100 + "%");
            db.put(t.get("id"), t);
        }
    }

    @Override
    protected Map<String, Map<String, String>> data() {
        return db;
    }

    @ModelField(id = true, name = {"编号", "en:ID"}, list = true)
    public String id;

    @ModelField(name = {"标题", "en:Title"}, list = true, search = true)
    public String title;

    @ModelField(name = {"负责人", "en:Assignee"}, list = true, search = true)
    public String assignee;

    @ModelField(name = {"分类", "en:Category"}, list = true)
    public String category;

    @ModelField(name = {"优先级", "en:Priority"}, list = true)
    public String priority;

    @ModelField(name = {"进度", "en:Progress"}, list = true)
    public String progress;
}
