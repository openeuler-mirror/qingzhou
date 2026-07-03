package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Monitor;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.store.MetricsStore;
import qingzhou.app.redis.store.model.MetricPoint;
import qingzhou.app.redis.util.RedisUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "appStatistics", icon = "Histogram", menu = "redis-monitor", order = 5,
        name = {"应用统计", "en:App Statistics"},
        info = {"Redis 应用关键统计指标趋势", "en:Key Redis application statistics and trends"})
public class AppStatistics extends RedisModelBase implements Monitor, qingzhou.api.type.List {

    
    @ModelField(field_type = FieldType.MONITORING,
            name = {"统计时间", "en:Stats Time"},
            info = {"统计时间", "en:Statistics time"})
    public String statsTime;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"当前总 Key 数", "en:Total Keys"},
            info = {"当前总 Key 数", "en:Current total keys"})
    public String totalKeys;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"当前 OPS", "en:Current OPS"},
            info = {"当前每秒操作数", "en:Current operations per second"})
    public String currentOps;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"当前连接数", "en:Current Connections"},
            info = {"当前连接数", "en:Current connected clients"})
    public String connectedClients;

    @ModelField(field_type = FieldType.MONITORING, chart_type = ChartType.stat,
            name = {"当前内存", "en:Current Memory"},
            info = {"当前已用内存", "en:Current used memory"})
    public String usedMemory;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.gauge,
            name = {"命中率(%)", "en:Hit Rate (%)"},
            info = {"缓存命中率", "en:Cache hit rate"})
    public String hitRate;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line,
            name = {"近 5 分钟平均 OPS", "en:Avg OPS (5m)"},
            info = {"近 5 分钟平均 OPS", "en:Average OPS in last 5 minutes"})
    public String avgOps5m;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line,
            name = {"近 1 小时内存增长(MB)", "en:Memory Growth (1h)"},
            info = {"近 1 小时内存增长", "en:Memory growth in last hour"})
    public String memoryGrowth1h;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"慢日志总数", "en:Slow Log Total"},
            info = {"当前慢日志总数", "en:Total slow logs"})
    public String slowLogTotal;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "latency",
            name = {"平均延迟(ms)", "en:Avg Latency (ms)"},
            info = {"最近探测平均延迟", "en:Average latency of recent probes"})
    public String avgLatency;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "latency",
            name = {"最大延迟(ms)", "en:Max Latency (ms)"},
            info = {"最近探测最大延迟", "en:Maximum latency of recent probes"})
    public String maxLatency;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.gauge, group = "usage",
            name = {"连接数使用率(%)", "en:Client Usage (%)"},
            info = {"当前连接数占 maxclients 比例", "en:Connected clients as percentage of maxclients"})
    public String clientUsageRate;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.gauge, group = "usage",
            name = {"内存使用率(%)", "en:Memory Usage (%)"},
            info = {"当前使用内存占 maxmemory 比例（无限制时显示 -1）", "en:Used memory as percentage of maxmemory"})
    public String memoryUsageRate;

    
    @ModelField(id = true, list = true,
            name = {"统计时间", "en:Stats Time"},
            info = {"指标统计时间", "en:Metric statistics time"})
    public String listStatsTime;

    @ModelField(list = true,
            name = {"指标名", "en:Metric Name"},
            info = {"指标名称", "en:Metric name"})
    public String metricName;

    @ModelField(list = true, numeric = true,
            name = {"指标值", "en:Metric Value"},
            info = {"指标值", "en:Metric value"})
    public String metricValue;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("statsTime", LocalDateTime.now().format(DATE_FORMAT));

        RedisUtil util = RedisApp.getRedisUtil();
        MetricsStore metricsStore = RedisApp.getMetricsStore();
        if (util == null || !util.isConnected() || metricsStore == null) {
            setUnavailable(data);
            return data;
        }

        Map<String, String> info = util.getAggregatedInfo();
        fillBaseMetrics(data, info);
        data.put("currentOps", info.getOrDefault("instantaneous_ops_per_sec", "0"));
        long usedMemBytes = Long.parseLong(info.getOrDefault("used_memory", "0"));
        data.put("usedMemory", RedisUtil.formatBytes(usedMemBytes));

        
        data.put("avgOps5m", String.format("%.2f", avgMetric(metricsStore, "redis.instantaneous_ops_per_sec", 5 * 60 * 1000)));

        
        long now = System.currentTimeMillis();
        List<MetricPoint> memPoints = metricsStore.query("redis.used_memory", now - 60 * 60 * 1000, now);
        double growthMb = 0;
        if (memPoints != null && memPoints.size() >= 2) {
            double first = memPoints.get(0).getValue();
            double last = memPoints.get(memPoints.size() - 1).getValue();
            growthMb = (last - first) / (1024 * 1024);
        }
        data.put("memoryGrowth1h", String.format("%.2f", growthMb));

        
        long slowLogLen = util.execute(jedis -> jedis.slowlogLen());
        data.put("slowLogTotal", String.valueOf(slowLogLen));

        List<MetricPoint> latencyPoints = metricsStore.query("redis.latency_ms",
                System.currentTimeMillis() - 5 * 60 * 1000, System.currentTimeMillis());
        long avg = 0, max = 0;
        if (latencyPoints != null && !latencyPoints.isEmpty()) {
            long sum = 0;
            for (MetricPoint point : latencyPoints) {
                sum += point.getValue();
                if (point.getValue() > max) {
                    max = (long) point.getValue();
                }
            }
            avg = sum / latencyPoints.size();
        }
        data.put("avgLatency", String.valueOf(avg));
        data.put("maxLatency", String.valueOf(max));

        long connected = Long.parseLong(info.getOrDefault("connected_clients", "0"));
        String maxClientsStr = util.configGetValue("maxclients");
        long maxClients = maxClientsStr != null && !maxClientsStr.isEmpty() ? Long.parseLong(maxClientsStr) : 10000;
        data.put("clientUsageRate", String.format("%.2f", connected * 100.0 / maxClients));

        long usedMemory = Long.parseLong(info.getOrDefault("used_memory", "0"));
        String maxMemoryStr = util.configGetValue("maxmemory");
        long maxMemory = maxMemoryStr != null && !maxMemoryStr.isEmpty() ? Long.parseLong(maxMemoryStr) : 0;
        data.put("memoryUsageRate", maxMemory > 0 ? String.format("%.2f", usedMemory * 100.0 / maxMemory) : "-1");

        return data;
    }

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        MetricsStore metricsStore = RedisApp.getMetricsStore();
        Map<String, List<MetricPoint>> latest = metricsStore.queryLatest(1);
        java.util.List<String[]> rows = new ArrayList<>();
        String time = LocalDateTime.now().format(DATE_FORMAT);
        for (Map.Entry<String, List<MetricPoint>> entry : latest.entrySet()) {
            List<MetricPoint> points = entry.getValue();
            if (points.isEmpty()) continue;
            MetricPoint point = points.get(points.size() - 1);
            rows.add(new String[]{time, entry.getKey(), String.valueOf(point.getValue())});
        }
        rows.sort((a, b) -> a[1].compareTo(b[1]));
        int start = (pageNum - 1) * pageSize;
        if (start >= rows.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, rows.size());
        return rows.subList(start, end);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        MetricsStore metricsStore = RedisApp.getMetricsStore();
        return metricsStore.getMetricNames().size();
    }

    @Override
    public boolean contains(String id) {
        return true;
    }

    private double avgMetric(MetricsStore store, String metric, long windowMs) {
        long now = System.currentTimeMillis();
        List<MetricPoint> points = store.query(metric, now - windowMs, now);
        if (points == null || points.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (MetricPoint point : points) {
            sum += point.getValue();
        }
        return sum / points.size();
    }

    private void setUnavailable(Map<String, String> data) {
        data.put("totalKeys", "-1");
        data.put("currentOps", "-1");
        data.put("connectedClients", "-1");
        data.put("usedMemory", "-1");
        data.put("hitRate", "-1");
        data.put("avgOps5m", "-1");
        data.put("memoryGrowth1h", "-1");
        data.put("slowLogTotal", "-1");
        data.put("avgLatency", "-1");
        data.put("maxLatency", "-1");
        data.put("clientUsageRate", "-1");
        data.put("memoryUsageRate", "-1");
    }
}
