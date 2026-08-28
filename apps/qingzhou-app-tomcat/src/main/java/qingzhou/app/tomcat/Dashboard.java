package qingzhou.app.tomcat;

import qingzhou.api.*;
import qingzhou.api.action.Show;
import java.util.HashMap;
import java.util.Map;

@Model(code = "dashboard",
        order = 0,
        name = {"首页", "en:Dashboard"},
        info = {"数据概览", "en:Dashboard Overview"},
        icon = "HomeFilled")
public class Dashboard extends qingzhou.api.ModelBase implements Show {

    @ModelField(id = true,
            name = {"编号", "en:ID"},
            show = false)
    public String id;

    @ModelField(
            name = {"安装路径", "en:Install Path"},
            info = {"Tomcat 安装路径", "en:Tomcat installation path"},
            group = "Basic")
    public String path;

    @Override
    public Map<String, String> show(String s) {
        Map<String, String> stats = new HashMap<>();
        stats.put("path",  getAppContext().getDetectedPath());
        return stats;
    }
}
