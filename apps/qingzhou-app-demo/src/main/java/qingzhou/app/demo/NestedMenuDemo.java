package qingzhou.app.demo;

import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Show;

/**
 * 三级菜单演示：挂在「高级功能 → 子菜单二 → 三级菜单示例」下，演示任意深度的菜单嵌套。
 */
@Model(code = "level3demo", order = 1, icon = "Star",
        name = {"三级菜单演示", "en:Level 3 Demo"},
        info = {"多级菜单嵌套演示", "en:Multi-level menu demo"},
        menu = "level3")
public class NestedMenuDemo extends qingzhou.api.ModelBase implements Show, Monitor {

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.monitor)
    public String statsTime;

    @ModelField(name = {"菜单层级", "en:Menu Level"}, info = {"当前菜单所在层级", "Current menu level"},
            field_type = FieldType.monitor, chart_type = ChartType.stat)
    public String menuLevel;

    @ModelField(name = {"演示说明", "en:Description"}, info = {"多级菜单功能说明", "Multi-level menu description"},
            field_type = FieldType.monitor)
    public String description;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> r = new HashMap<>();
        r.put("statsTime", DateKit.now());
        r.put("menuLevel", "3");
        r.put("description", "这是一个三级菜单示例，展示了Qingzhou平台支持任意深度的菜单嵌套");
        return r;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }
}
