package qingzhou.app.nginx;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Monitor;

import java.lang.management.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Model(code = "home", order = 1, icon = "HomeFilled", menu = "basic", action = "monitor",
        name = {"首页", "en:Home Page"}, info = {"首页", "en:Home Page"})
public class Home extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"},
            field_type = FieldType.MONITORING)
    public String statsTime;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"应用运行时长", "en:Duration"}, info = {"应用运行时长", "en:App run duration"})
    public String duration;

    @Override
    public Map<String, String> monitor(Request request) throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("statsTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        data.put("duration", formatUptime(runtimeMXBean.getUptime()));

        return data;
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m");
        }
        sb.append(secs).append("s");
        return sb.toString();
    }
}
