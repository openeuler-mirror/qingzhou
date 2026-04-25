package qingzhou.app.nginx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Monitor;

/**
 * Dashboard 首页模型 提供实时统计数据展示
 */
@Model(code = "nginxStatus", order = 2, icon = "Monitor", action = "monitor", menu = "basic",
        name = {"Nginx 连接状态", "en:Nginx Connections"},
        info = {"Nginx 连接状态", "en:Nginx Connections"})
public class NginxStatus extends qingzhou.api.ModelBase implements Monitor {

    @ModelField(field_type = FieldType.MONITORING,
            name = {"统计时间", "en:Stats Time"},
            info = {"数据统计时间", "en:Statistics generation time"})
    public String statsTime;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"活动连接数", "en:Active Connections"},
            info = {"当前所有处于打开状态的活动连接数", "en:Active connections statistics"})
    public String activeConnections;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"接收连接数", "en:Accepts"}, info = {"已经接收连接数", "en:Total accepts"})
    public String accepts;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"处理连接数", "en:Handled"},
            info = {"已经处理过的连接数", "en:Total handled connections"})
    public String handled;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"处理请求数", "en:Requests"},
            info = {"已经处理过的请求数，在保持连接模式下，请求数量可能会大于连接数量", "en:Total requests handled"})
    public String requests;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"接收请求中连接", "en:Connections currently reading requests"},
            info = {"正处于接收请求的连接数", "en:Connections currently reading requests"})
    public String reading;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"响应请求中连接", "en:Connections currently writing responses"},
            info = {"请求已经接收完成，处于响应过程的连接数", "en:Connections currently writing responses"})
    public String writing;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"活动状态的连接", "en:Connections in keep-alive state"},
            info = {"保持连接模式，处于活动状态的连接数", "en:Connections in keep-alive state"})
    public String waiting;

    @Override
    public Map<String, String> monitor(Request request) {
        Map<String, String> stats = new HashMap<>();
        stats.put("statsTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 1. 请求 Nginx status
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(AppConfig.getNginxStatusUrl())).build();
        try {
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            /* 响应文本格式：
            Active connections: 1 
            server accepts handled requests
            1 1 1 
            Reading: 0 Writing: 1 Waiting: 0
             */
            String[] lines = response.body().split("\n");

            // Active connections: 1
            stats.put("activeConnections", lines[0].replaceAll("[^0-9]", ""));

            // accepts handled requests
            String[] reqs = lines[2].trim().split(" +");
            stats.put("accepts", reqs[0]);
            stats.put("handled", reqs[1]);
            stats.put("requests", reqs[2]);

            // Reading: 0 Writing: 1 Waiting: 1
            String[] conn = lines[3].trim().split(" +");
            stats.put("reading", conn[1]);
            stats.put("writing", conn[3]);
            stats.put("waiting", conn[5]);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return stats;
    }
}
