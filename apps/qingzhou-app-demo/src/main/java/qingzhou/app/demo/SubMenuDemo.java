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
 * 二级菜单演示模型
 */
@Model(code = "sub1demo", order = 1, icon = "Document",
        name = {"二级菜单示例", "en:Level 2 Demo"},
        info = {"二级菜单演示", "en:Second level menu demo"},
        menu = "sub1")
public class SubMenuDemo extends qingzhou.api.ModelBase implements Show, Monitor {

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

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> result = new HashMap<>();

        result.put("statsTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        result.put("menuLevel", "2");

        return result;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }
}
