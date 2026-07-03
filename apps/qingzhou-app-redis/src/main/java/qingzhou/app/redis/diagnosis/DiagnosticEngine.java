package qingzhou.app.redis.diagnosis;

import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.store.DiagnosisStore;
import qingzhou.app.redis.store.MetricsStore;
import qingzhou.app.redis.store.model.DiagnosisReport;
import qingzhou.app.redis.util.RedisUtil;

import java.util.*;

public class DiagnosticEngine implements Runnable {

    private static volatile DiagnosticEngine instance;

    private final DiagnosisStore diagnosisStore;
    private final MetricsStore metricsStore;

    public DiagnosticEngine(DiagnosisStore diagnosisStore, MetricsStore metricsStore) {
        this.diagnosisStore = diagnosisStore;
        this.metricsStore = metricsStore;
    }

    public static DiagnosticEngine initialize(DiagnosisStore diagnosisStore, MetricsStore metricsStore) {
        instance = new DiagnosticEngine(diagnosisStore, metricsStore);
        return instance;
    }

    public static DiagnosticEngine getInstance() {
        return instance;
    }

    @Override
    public void run() {
        RedisUtil util = RedisApp.getRedisUtil();
        if (util == null || !util.isConnected()) {
            return;
        }
        diagnose(util);
    }

    public void diagnose(RedisUtil util) {
        long now = System.currentTimeMillis();
        List<DiagnosisReport> reports = new ArrayList<>();

        try {
            Map<String, String> info = util.getAggregatedInfo();


            double frag = parseDouble(info.get("mem_fragmentation_ratio"));
            if (frag > 1.5) {
                reports.add(buildReport(now, "内存", "内存碎片率偏高",
                        "当前内存碎片率 " + String.format("%.2f", frag) + "，建议适时重启或触发内存整理（若版本支持）。",
                        "used_memory_rss 与 used_memory 比值过高，可能因大量 Key 过期/删除导致。",
                        "警告", "考虑在低峰期重启 Redis 或升级版本使用 activedefrag。"));
            }


            double hitRate = parseDouble(info.get("hit_rate"));
            if (hitRate < 50) {
                reports.add(buildReport(now, "性能", "缓存命中率偏低",
                        "当前缓存命中率 " + String.format("%.2f", hitRate) + "%，缓存效果不佳。",
                        "大量请求未命中缓存，可能是缓存穿透或缓存预热不足。",
                        "警告", "检查缓存 Key 设计，增加预热，或引入布隆过滤器。"));
            }


            long connectedClients = parseLong(info.get("connected_clients"));
            long maxClients = getMaxClients(util);
            if (maxClients > 0 && connectedClients > maxClients * 0.8) {
                reports.add(buildReport(now, "连接", "连接数接近上限",
                        "当前连接数 " + connectedClients + "，接近 maxclients " + maxClients + "。",
                        "客户端连接持续增长可能耗尽连接资源。",
                        "警告", "检查客户端连接池配置，排查连接泄漏，必要时扩容。"));
            }


            Double latency = metricsStore.getLatest("redis.latency_ms");
            if (latency != null && latency > 100) {
                reports.add(buildReport(now, "性能", "Redis 响应延迟过高",
                        "最近探测延迟 " + latency.longValue() + " ms，超过 100 ms 阈值。",
                        "可能因慢查询、大 Key、CPU 瓶颈或网络抖动导致。",
                        "严重", "排查慢日志与大 Key，优化命令，检查宿主机资源。"));
            }


            long evicted = parseLong(info.get("evicted_keys"));
            if (evicted > 0) {
                reports.add(buildReport(now, "内存", "存在 Key 被驱逐",
                        "累计驱逐 Key 数量 " + evicted + "，内存可能不足。",
                        "达到 maxmemory 后触发淘汰策略。",
                        "警告", "评估内存容量，调整 maxmemory 策略，清理无用数据。"));
            }
        } catch (Exception e) {
        }


        try {
            diagnoseBigKeys(util, now, reports);
        } catch (Exception e) {
        }

        for (DiagnosisReport report : reports) {
            diagnosisStore.add(report);
        }
    }

    private void diagnoseBigKeys(RedisUtil util, long now, List<DiagnosisReport> reports) {
        List<Map.Entry<String, Long>> bigKeys = util.findBigKeys(10);
        if (bigKeys != null && !bigKeys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(5, bigKeys.size()); i++) {
                Map.Entry<String, Long> entry = bigKeys.get(i);
                sb.append(entry.getKey()).append("(").append(entry.getValue()).append(" bytes) ");
            }
            reports.add(buildReport(now, "内存", "发现大 Key",
                    "Top 大 Key: " + sb,
                    "单个 Key 内存占用过大，可能阻塞主线程或导致延迟尖刺。",
                    "警告", "拆分大 Key、使用 Hash 分片，或降低单个 Value 大小。"));
        }
    }

    private long getMaxClients(RedisUtil util) {
        try {
            Map<String, String> config = util.execute(jedis -> jedis.configGet("maxclients"));
            if (config != null && config.containsKey("maxclients")) {
                return Long.parseLong(config.get("maxclients"));
            }
        } catch (Exception ignored) {
        }
        return 10000;
    }

    private DiagnosisReport buildReport(long now, String category, String title,
                                        String description, String cause, String level, String suggestion) {
        DiagnosisReport report = new DiagnosisReport();
        report.setTimestamp(now);
        report.setCategory(category);
        report.setTitle(title);
        report.setDescription(description);
        report.setCause(cause);
        report.setLevel(level);
        report.setSuggestion(suggestion);
        report.setStatus("未处理");
        return report;
    }

    private double parseDouble(String value) {
        try {
            return value == null ? 0 : Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
