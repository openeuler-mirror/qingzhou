package qingzhou.app.demo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Monitor;
import qingzhou.api.type.Show;

/**
 * 多级菜单演示模型
 * 演示三级菜单嵌套：高级功能 → 子菜单二 → 三级菜单示例
 */
@Model(code = "level3demo", order = 1, icon = "Star",
        name = {"三级菜单演示", "en:Level 3 Demo"},
        info = {"多级菜单嵌套演示", "en:Multi-level menu demo"},
        menu = "level3")
public class NestedMenuDemo extends qingzhou.api.ModelBase implements Show, Monitor {

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.MONITORING)
    public String statsTime;

    @ModelField(
            name = {"菜单层级", "en:Menu Level"},
            info = {"当前菜单所在层级", "Current menu level"},
            field_type = FieldType.MONITORING,
            chart_type = ChartType.stat)
    public String menuLevel;

    @ModelField(
            name = {"演示说明", "en:Description"},
            info = {"多级菜单功能说明", "Multi-level menu description"},
            field_type = FieldType.MONITORING)
    public String description;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> result = new HashMap<>();

        result.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        result.put("menuLevel", "3");
        result.put("description", "这是一个三级菜单示例，展示了Qingzhou平台支持任意深度的菜单嵌套");

        return result;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }
}
