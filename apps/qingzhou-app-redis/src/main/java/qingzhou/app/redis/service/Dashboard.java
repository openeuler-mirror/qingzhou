package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Monitor;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.util.RedisUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "dashboard", icon = "Odometer", menu = "redis-monitor", order = 1,
        name = {"监控面板", "en:Dashboard"},
        info = {"Redis 服务器核心指标监控面板。", "en:Redis server key metrics dashboard."})
public class Dashboard extends RedisModelBase implements Monitor {

    @ModelField(field_type = FieldType.MONITORING,
            name = {"统计时间", "en:Stats Time"},
            info = {"数据采集时间", "en:Data collection time"})
    public String statsTime;

    @ModelField(field_type = FieldType.MONITORING,
            name = {"Redis 版本", "en:Redis Version"},
            info = {"Redis 服务器版本号", "en:Redis server version"})
    public String redisVersion;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat,
            name = {"运行时长(天)", "en:Uptime(days)"},
            info = {"Redis 服务器运行时长", "en:Redis server uptime"})
    public String uptime;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "clients",
            name = {"连接数", "en:Connected Clients"},
            info = {"当前连接的客户端数量", "en:Number of connected clients"})
    public String connectedClients;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "clients",
            name = {"阻塞客户端数", "en:Blocked Clients"},
            info = {"阻塞中的客户端数量", "en:Number of blocked clients"})
    public String blockedClients;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "clients",
            name = {"拒绝连接数", "en:Rejected Connections"},
            info = {"因超过 maxclients 被拒绝的连接数", "en:Connections rejected due to maxclients"})
    public String rejectedConnections;

    @ModelField(field_type = FieldType.MONITORING, chart_type = ChartType.stat, group = "memory",
            name = {"已用内存", "en:Used Memory"},
            info = {"Redis 分配的已用内存（可读）", "en:Used memory in human readable format"})
    public String usedMemoryHuman;

    @ModelField(field_type = FieldType.MONITORING, chart_type = ChartType.stat, group = "memory",
            name = {"物理内存", "en:RSS Memory"},
            info = {"操作系统分配给 Redis 的物理内存（自动转换 MB/GB）", "en:Physical memory allocated by OS"})
    public String usedMemoryRss;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "memory",
            name = {"内存碎片率", "en:Fragmentation Ratio"},
            info = {"内存碎片率（used_memory_rss / used_memory）", "en:Memory fragmentation ratio"})
    public String memFragmentationRatio;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "keys",
            name = {"Key 总数", "en:Total Keys"},
            info = {"所有数据库中的 Key 总数", "en:Total number of keys across all databases"})
    public String totalKeys;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "quality",
            name = {"命中率(%)", "en:Hit Rate(%)"},
            info = {"缓存命中率（keyspace_hits / total_ops）", "en:Cache hit rate"})
    public String hitRate;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.line, group = "ops",
            name = {"每秒操作数", "en:Ops/Sec"},
            info = {"每秒执行的操作数", "en:Number of operations per second"})
    public String opsPerSec;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "keys",
            name = {"过期 Key 数", "en:Expired Keys"},
            info = {"已过期的 Key 数量", "en:Number of expired keys"})
    public String expiredKeys;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "keys",
            name = {"驱逐 Key 数", "en:Evicted Keys"},
            info = {"因内存不足被驱逐的 Key 数量", "en:Number of evicted keys due to maxmemory"})
    public String evictedKeys;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "keys",
            name = {"命中次数", "en:Keyspace Hits"},
            info = {"缓存命中次数", "en:Number of successful key lookups"})
    public String keyspaceHits;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "keys",
            name = {"未命中次数", "en:Keyspace Misses"},
            info = {"缓存未命中次数", "en:Number of failed key lookups"})
    public String keyspaceMisses;

    @ModelField(field_type = FieldType.MONITORING, numeric = true, chart_type = ChartType.stat, group = "clients",
            name = {"Pubsub 通道数", "en:Pubsub Channels"},
            info = {"当前 Pubsub 通道数", "en:Current pubsub channels"})
    public String pubsubChannels;

    @Override
    public Map<String, String> monitor(String id) throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("statsTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Map<String, String> info = getRedisUtil().getAggregatedInfo();
        fillBaseMetrics(data, info);

        data.put("redisVersion", info.getOrDefault("redis_version", "-"));
        long uptimeSec = Long.parseLong(info.getOrDefault("uptime_in_seconds", "0"));
        data.put("uptime", String.format("%.1f", uptimeSec / 86400.0));
        data.put("blockedClients", info.getOrDefault("blocked_clients", "0"));
        data.put("rejectedConnections", info.getOrDefault("rejected_connections", "0"));
        data.put("usedMemoryHuman", info.getOrDefault("used_memory_human", "-"));
        data.put("usedMemoryRss", RedisUtil.formatBytes(Long.parseLong(info.getOrDefault("used_memory_rss", "0"))));
        data.put("memFragmentationRatio", info.getOrDefault("mem_fragmentation_ratio", "0"));
        data.put("opsPerSec", info.getOrDefault("instantaneous_ops_per_sec", "0"));
        data.put("expiredKeys", info.getOrDefault("expired_keys", "0"));
        data.put("evictedKeys", info.getOrDefault("evicted_keys", "0"));
        data.put("keyspaceHits", info.getOrDefault("keyspace_hits", "0"));
        data.put("keyspaceMisses", info.getOrDefault("keyspace_misses", "0"));
        data.put("pubsubChannels", info.getOrDefault("pubsub_channels", "0"));

        return data;
    }
}