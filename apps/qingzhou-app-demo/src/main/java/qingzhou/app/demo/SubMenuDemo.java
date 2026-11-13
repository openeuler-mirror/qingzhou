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
 * 二级菜单演示：挂在「高级功能 → 子菜单一」下。
 */
@Model(code = "sub1demo", order = 1, icon = "Document",
        name = {"二级菜单示例", "en:Level 2 Demo"},
        info = {"二级菜单演示", "en:Second level menu demo"},
        menu = "sub1")
public class SubMenuDemo extends qingzhou.api.ModelBase implements Show, Monitor {

    @ModelField(name = {"统计时间", "en:Stats Time"}, info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.monitor)
    public String statsTime;

    @ModelField(name = {"菜单层级", "en:Menu Level"}, info = {"当前菜单所在层级", "Current menu level"},
            field_type = FieldType.monitor, chart_type = ChartType.stat)
    public String menuLevel;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> r = new HashMap<>();
        r.put("statsTime", DateKit.now());
        r.put("menuLevel", "2");
        return r;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }
}
