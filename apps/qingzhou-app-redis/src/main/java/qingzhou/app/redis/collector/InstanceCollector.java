package qingzhou.app.redis.collector;

import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.store.MetricsStore;
import qingzhou.app.redis.util.RedisUtil;

import java.util.Map;

public class InstanceCollector {

    private final MetricsStore metricsStore;

    public InstanceCollector(MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    public void collect() {
        RedisUtil util = RedisApp.getRedisUtil();
        if (util == null || !util.isConnected()) {
            return;
        }
        try {
            Map<String, String> info = util.getAggregatedInfo();
            long now = System.currentTimeMillis();
            record("redis.used_memory", info.get("used_memory"), now);
            record("redis.used_memory_rss", info.get("used_memory_rss"), now);
            record("redis.connected_clients", info.get("connected_clients"), now);
            record("redis.instantaneous_ops_per_sec", info.get("instantaneous_ops_per_sec"), now);
            record("redis.keyspace_hits", info.get("keyspace_hits"), now);
            record("redis.keyspace_misses", info.get("keyspace_misses"), now);
            record("redis.expired_keys", info.get("expired_keys"), now);
            record("redis.evicted_keys", info.get("evicted_keys"), now);
            record("redis.total_keys", info.get("total_keys"), now);
            record("redis.rejected_connections", info.get("rejected_connections"), now);
            record("redis.hit_rate", info.get("hit_rate"), now);
            record("redis.mem_fragmentation_ratio", info.get("mem_fragmentation_ratio"), now);


            long latency = util.latencyPing();
            metricsStore.record("redis.latency_ms", latency, now);
        } catch (Exception e) {
        }
    }

    private void record(String metric, String value, long timestamp) {
        if (metric == null || value == null || value.isEmpty()) {
            return;
        }
        try {
            double v = Double.parseDouble(value);
            metricsStore.record(metric, v, timestamp);
        } catch (NumberFormatException ignored) {
        }
    }
}
