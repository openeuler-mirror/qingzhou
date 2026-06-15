package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.List;

/**
 * 演示无行操作的纯列表页面（仅实现List接口，不实现Show/Update/Delete）
 * 验证：列表页无"操作"列，分页正常显示
 */
@Model(code = "simple-list", order = 12,
        name = {"无操作列表", "en:No-Op List"},
        info = {"演示纯列表页（无行操作），25条数据验证分页和无操作列", "en:Demo list-only page with pagination, no row actions"},
        icon = "List",
        menu = "advanced")
public class SimpleListDemo extends qingzhou.api.ModelBase implements List {

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

    @ModelField(id = true,
            name = {"编号", "en:ID"},
            list = true)
    public String id;

    @ModelField(
            name = {"标题", "en:Title"},
            list = true,
            search = true)
    public String title;

    @ModelField(
            name = {"负责人", "en:Assignee"},
            list = true,
            search = true)
    public String assignee;

    @ModelField(
            name = {"分类", "en:Category"},
            list = true)
    public String category;

    @ModelField(
            name = {"优先级", "en:Priority"},
            list = true)
    public String priority;

    @ModelField(
            name = {"进度", "en:Progress"},
            list = true)
    public String progress;

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> result = new ArrayList<>();
        java.util.List<Map<String, String>> filtered = new ArrayList<>();

        for (Map<String, String> task : db.values()) {
            if (matchesQuery(task, query)) {
                filtered.add(task);
            }
        }

        int fromIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(fromIndex + pageSize, filtered.size());

        for (int i = fromIndex; i < endIndex; i++) {
            Map<String, String> t = filtered.get(i);
            String[] data = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String value = t.get(listFields[j]);
                data[j] = value != null ? value : "";
            }
            result.add(data);
        }

        return result;
    }

    private boolean matchesQuery(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;

        for (Map.Entry<String, String> entry : query.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                String dataValue = data.get(key);
                if (dataValue == null || !dataValue.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        int count = 0;
        for (Map<String, String> task : db.values()) {
            if (matchesQuery(task, query)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean contains(String id) {
        return db.containsKey(id);
    }
}