package qingzhou.app.nacos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.*;
import qingzhou.api.action.Monitor;
import qingzhou.api.action.Show;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelField;

@Model(code = "nacos-dashboard", order = 1, icon = "HomeFilled",
        name = {"Nacos仪表盘", "en:Nacos Dashboard"}, info = {"Nacos服务概览仪表盘", "en:Nacos Service Overview Dashboard"})
public class NacosDashboard extends NacosModelBase implements Show, Monitor {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ModelField(id = true, name = {"编号", "en:ID"}, field_type = FieldType.monitor, show = true)
    public String id;
    @ModelField(name = {"统计时间", "en:Stats Time"}, field_type = FieldType.monitor, show = true)
    public String statsTime;
    @ModelField(name = {"安装路径", "en:Install Path"}, info = {"安装路径", "en:installation path"}, field_type = FieldType.monitor, group = "SystemInfo")
    public String path;

    @Override
    public Map<String, String> monitor(String id) {
        Map<String, String> stats = new HashMap<>();
        stats.put("statsTime", TIME.format(LocalDateTime.now()));
        stats.put("id", "1");
        stats.put("path", "unkown");
        return stats;
    }

    @Override
    public Map<String, String> show(String id) {
        return monitor(id);
    }
}
