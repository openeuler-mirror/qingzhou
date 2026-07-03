package qingzhou.app.redis.util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisUtil {

    private String mode;
    private String host;
    private int port;
    private String password;
    private int database;
    private int timeout = 60000;
    private Set<String> sentinelNodes;
    private String sentinelMaster;
    private String clusterNodesStr;
    private String sentinelNodesStr;


    private JedisPool jedisPool;
    private JedisSentinelPool jedisSentinelPool;
    private JedisCluster jedisCluster;

    private volatile boolean connected = false;
    private volatile String redisVersion;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = 30000;

    public void init(Properties props) {
        mode = props.getProperty("redis.mode", "standalone");
        password = props.getProperty("redis.password", "");
        if (password == null) password = "";
        host = props.getProperty("redis.host", "127.0.0.1");
        port = Integer.parseInt(props.getProperty("redis.port", "6379"));
        database = Integer.parseInt(props.getProperty("redis.database", "0"));
        sentinelMaster = props.getProperty("redis.sentinel.master", "mymaster");
        sentinelNodesStr = props.getProperty("redis.sentinel.nodes", "");
        clusterNodesStr = props.getProperty("redis.cluster.nodes", "");
        initConnection();
    }

    public void init(Map<String, String> config) {
        mode = config.getOrDefault("mode", "standalone");
        password = config.getOrDefault("password", "");
        if (password == null) password = "";
        host = config.getOrDefault("host", "127.0.0.1");
        port = Integer.parseInt(config.getOrDefault("port", "6379"));
        database = Integer.parseInt(config.getOrDefault("database", "0"));
        sentinelMaster = config.getOrDefault("sentinelMaster", "mymaster");
        sentinelNodesStr = config.getOrDefault("sentinelNodes", "");
        clusterNodesStr = config.getOrDefault("clusterNodes", "");
        initConnection();
    }

    private void initConnection() {
        close();

        try {
            if ("cluster".equals(mode)) {
                if (database != 0) {
                    throw new JedisException("Redis Cluster 只支持 database 0，当前配置 database=" + database);
                }
                initClusterPool();
            } else if ("sentinel".equals(mode)) {
                initSentinelPool();
            } else {
                initStandalonePool();
            }


            if ("cluster".equals(mode)) {
                jedisCluster.ping();
                redisVersion = getClusterVersion();
            } else {
                execute(jedis -> {
                    jedis.ping();
                    String info = jedis.info("Server");
                    redisVersion = extractVersionFromInfo(info);
                    return null;
                });
            }
            connected = true;
            lastHealthCheckTime = System.currentTimeMillis();
        } catch (Exception e) {
            connected = false;
            redisVersion = null;
            throw new JedisException("Redis 连接失败: " + e.getMessage(), e);
        }
    }

    private String getClusterVersion() {
        try {
            Map<String, ConnectionPool> nodes = jedisCluster.getClusterNodes();
            if (!nodes.isEmpty()) {
                try (Jedis jedis = new Jedis(nodes.values().iterator().next().getResource())) {
                    String info = jedis.info("Server");
                    return extractVersionFromInfo(info);
                }
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String extractVersionFromInfo(String info) {
        if (info == null) return "unknown";
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("redis_version:")) {
                return line.substring("redis_version:".length()).trim();
            }
        }
        return "unknown";
    }

    private void initStandalonePool() {
        JedisPoolConfig poolConfig = createPoolConfig();
        if (!password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, password, database);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, timeout, null, database);
        }
    }

    private void initSentinelPool() {
        Set<String> sentinels = new HashSet<>();
        if (sentinelNodesStr != null && !sentinelNodesStr.isEmpty()) {
            for (String node : sentinelNodesStr.split("[,\n]")) {
                String trimmed = node.trim();
                if (!trimmed.isEmpty()) sentinels.add(trimmed);
            }
        }
        this.sentinelNodes = sentinels;

        JedisPoolConfig poolConfig = createPoolConfig();
        if (!password.isEmpty()) {
            jedisSentinelPool = new JedisSentinelPool(sentinelMaster, sentinels, poolConfig, timeout, password, database);
        } else {
            jedisSentinelPool = new JedisSentinelPool(sentinelMaster, sentinels, poolConfig, timeout, null, database);
        }
    }

    private void initClusterPool() {
        Set<HostAndPort> nodes = new HashSet<>();
        if (clusterNodesStr != null && !clusterNodesStr.isEmpty()) {
            for (String node : clusterNodesStr.split("[,\n]")) {
                String[] parts = node.trim().split(":");
                if (parts.length >= 2) {
                    nodes.add(new HostAndPort(parts[0], Integer.parseInt(parts[1])));
                }
            }
        }

        GenericObjectPoolConfig<Connection> poolConfig = createClusterPoolConfig();
        if (!password.isEmpty()) {
            jedisCluster = new JedisCluster(nodes, timeout, timeout, 5, password, poolConfig);
        } else {
            jedisCluster = new JedisCluster(nodes, timeout, poolConfig);
        }
    }

    private JedisPoolConfig createPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setMaxWaitMillis(5000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        return poolConfig;
    }




    private GenericObjectPoolConfig<Connection> createClusterPoolConfig() {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setMaxWaitMillis(5000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        return poolConfig;
    }

    public boolean isCluster() {
        return "cluster".equals(mode);
    }

    public String getMode() {
        return mode;
    }




    public String getRedisVersion() {
        return redisVersion != null ? redisVersion : "unknown";
    }




    public long latencyPing() {
        long start = System.currentTimeMillis();
        try {
            if (isCluster()) {
                jedisCluster.ping();
            } else {
                execute(jedis -> {
                    jedis.ping();
                    return null;
                });
            }
        } catch (Exception e) {
            return -1;
        }
        return System.currentTimeMillis() - start;
    }




    public Map<String, String> infoSection(String section) {
        if (isCluster()) {
            return executeOnFirstClusterNode(jedis -> parseInfo(section, jedis.info(section)));
        }
        return execute(jedis -> parseInfo(section, jedis.info(section)));
    }




    public String configGetValue(String key) {
        try {
            if (isCluster()) {
                return executeOnFirstClusterNode(jedis -> {
                    Map<String, String> result = jedis.configGet(key);
                    return result != null ? result.get(key) : null;
                });
            }
            return execute(jedis -> {
                Map<String, String> result = jedis.configGet(key);
                return result != null ? result.get(key) : null;
            });
        } catch (Exception e) {
            return null;
        }
    }




    public Map<String, String> nodeHealth() {
        Map<String, String> result = new LinkedHashMap<>();
        if (isCluster()) {
            Map<String, ConnectionPool> nodes = jedisCluster.getClusterNodes();
            for (Map.Entry<String, ConnectionPool> entry : nodes.entrySet()) {
                String addr = entry.getKey();
                try (Jedis jedis = new Jedis(entry.getValue().getResource())) {
                    long start = System.currentTimeMillis();
                    jedis.ping();
                    long latency = System.currentTimeMillis() - start;
                    String info = jedis.info("Replication");
                    String role = "unknown";
                    for (String line : info.split("\r?\n")) {
                        if (line.startsWith("role:")) {
                            role = line.substring(5).trim();
                            break;
                        }
                    }
                    result.put(addr, addr + "|" + role + "|ok|" + latency + "ms");
                } catch (Exception e) {
                    result.put(addr, addr + "|unknown|error|" + e.getMessage());
                }
            }
        } else {
            try {
                long latency = latencyPing();
                String status = latency >= 0 ? "ok" : "error";
                result.put(host + ":" + port, host + ":" + port + "|master|" + status + "|" + (latency >= 0 ? latency + "ms" : "-1"));
            } catch (Exception e) {
                result.put(host + ":" + port, host + ":" + port + "|master|error|" + e.getMessage());
            }
        }
        return result;
    }




    public List<Map.Entry<String, Long>> findBigKeys(int sampleSize) {
        List<Map.Entry<String, Long>> result = new ArrayList<>();
        if (sampleSize <= 0) {
            return result;
        }
        try {
            if (isCluster()) {
                executeOnAllClusterNodes(jedis -> collectBigKeys(jedis, sampleSize, result));
            } else {
                execute(jedis -> {
                    collectBigKeys(jedis, sampleSize, result);
                    return null;
                });
            }
        } catch (Exception ignored) {
        }
        result.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return result;
    }

    private void collectBigKeys(Jedis jedis, int sampleSize, List<Map.Entry<String, Long>> result) {
        ScanParams params = new ScanParams();
        params.count(100);
        String cursor = "0";
        int sampled = 0;
        do {
            try {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                cursor = scanResult.getCursor();
                List<String> keys = scanResult.getResult();
                for (String key : keys) {
                    try {
                        Long usage = jedis.memoryUsage(key);
                        if (usage != null) {
                            synchronized (result) {
                                result.add(new AbstractMap.SimpleEntry<>(key, usage));
                                if (result.size() > sampleSize * 2) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    sampled++;
                    if (sampled >= sampleSize * 10) {
                        cursor = "0";
                        break;
                    }
                }
            } catch (Exception e) {
                break;
            }
        } while (!"0".equals(cursor));
    }




    public boolean isVersionAtLeast(String minVersion) {
        String current = getRedisVersion();
        if ("unknown".equals(current)) return true;
        return compareVersions(current, minVersion) >= 0;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.split("[^0-9]")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isConnected() {
        if (!connected) return false;
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > HEALTH_CHECK_INTERVAL) {
            try {
                if (isCluster()) {
                    jedisCluster.ping();
                } else {
                    execute(jedis -> jedis.ping());
                }
                lastHealthCheckTime = now;
                return true;
            } catch (Exception e) {
                connected = false;
                return false;
            }
        }
        return true;
    }





    public <T> T execute(Function<Jedis, T> function) {
        try (Jedis jedis = getJedis()) {
            return function.apply(jedis);
        }
    }




    private Jedis getJedis() {
        if (jedisSentinelPool != null) return jedisSentinelPool.getResource();
        if (jedisPool != null) return jedisPool.getResource();
        throw new JedisException("Redis 连接池未初始化");
    }




    public <T> T executeWithCluster(Function<JedisCluster, T> function) {
        if (jedisCluster == null) {
            throw new JedisException("Redis 集群连接未初始化");
        }
        return function.apply(jedisCluster);
    }





    public void executeOnAllClusterNodes(Consumer<Jedis> consumer) {
        if (jedisCluster == null) {
            throw new JedisException("Redis 集群连接未初始化");
        }
        Map<String, ConnectionPool> clusterNodes = jedisCluster.getClusterNodes();
        for (Map.Entry<String, ConnectionPool> entry : clusterNodes.entrySet()) {
            if (isReplicaNode(entry.getKey())) continue;
            try (Jedis jedis = new Jedis(entry.getValue().getResource())) {
                consumer.accept(jedis);
            } catch (Exception ignored) {
            }
        }
    }




    public void executeOnAllClusterNodesIncludingReplicas(Consumer<Jedis> consumer) {
        if (jedisCluster == null) {
            throw new JedisException("Redis 集群连接未初始化");
        }
        Map<String, ConnectionPool> clusterNodes = jedisCluster.getClusterNodes();
        for (Map.Entry<String, ConnectionPool> entry : clusterNodes.entrySet()) {
            try (Jedis jedis = new Jedis(entry.getValue().getResource())) {
                consumer.accept(jedis);
            } catch (Exception ignored) {
            }
        }
    }




    private volatile Set<String> replicaNodeIds;

    private boolean isReplicaNode(String nodeKey) {
        if (replicaNodeIds == null) {
            refreshReplicaInfo();
        }
        return replicaNodeIds != null && replicaNodeIds.contains(nodeKey);
    }

    private void refreshReplicaInfo() {
        try {
            if (jedisCluster == null) return;
            Set<String> replicas = new HashSet<>();
            Map<String, ConnectionPool> nodes = jedisCluster.getClusterNodes();
            if (nodes.isEmpty()) return;

            for (ConnectionPool pool : nodes.values()) {
                try (Jedis jedis = new Jedis(pool.getResource())) {
                    String clusterInfo = jedis.clusterNodes();
                    for (String line : clusterInfo.split("\n")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            String addr = parts[1].split("@")[0];
                            String flags = parts[2];
                            if (flags.contains("slave")) {
                                replicas.add(addr);
                            }
                        }
                    }
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
            replicaNodeIds = replicas;
        } catch (Exception ignored) {
        }
    }




    public <T> T executeOnFirstClusterNode(Function<Jedis, T> function) {
        if (jedisCluster == null) {
            throw new JedisException("Redis 集群连接未初始化");
        }
        Map<String, ConnectionPool> clusterNodes = jedisCluster.getClusterNodes();
        if (clusterNodes.isEmpty()) {
            throw new JedisException("集群无可用节点");
        }
        for (Map.Entry<String, ConnectionPool> entry : clusterNodes.entrySet()) {
            if (!isReplicaNode(entry.getKey())) {
                try (Jedis jedis = new Jedis(entry.getValue().getResource())) {
                    return function.apply(jedis);
                }
            }
        }
        ConnectionPool firstPool = clusterNodes.values().iterator().next();
        try (Jedis jedis = new Jedis(firstPool.getResource())) {
            return function.apply(jedis);
        }
    }

    public void close() {
        connected = false;
        redisVersion = null;
        replicaNodeIds = null;
        try {
            if (jedisPool != null) {
                jedisPool.close();
                jedisPool = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (jedisSentinelPool != null) {
                jedisSentinelPool.close();
                jedisSentinelPool = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (jedisCluster != null) {
                jedisCluster.close();
                jedisCluster = null;
            }
        } catch (Exception ignored) {
        }
    }

    public Map<String, String> parseInfo(String section, String info) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] lines = info.split("\r?\n");
        boolean inSection = false;
        for (String line : lines) {
            if (line.startsWith("# ")) {
                inSection = line.substring(2).trim().equalsIgnoreCase(section);
                continue;
            }
            if (inSection && !line.trim().isEmpty()) {
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    result.put(line.substring(0, colonIdx), line.substring(colonIdx + 1));
                }
            }
        }
        return result;
    }

    public Map<String, String> getAggregatedInfo() {
        Map<String, String> aggregated = new LinkedHashMap<>();
        if (isCluster()) {
            executeOnAllClusterNodes(jedis -> {
                try {
                    String info = jedis.info();
                    Map<String, String> serverInfo = parseInfo("Server", info);
                    Map<String, String> clientsInfo = parseInfo("Clients", info);
                    Map<String, String> memoryInfo = parseInfo("Memory", info);
                    Map<String, String> statsInfo = parseInfo("Stats", info);
                    Map<String, String> keyspaceInfo = parseInfo("Keyspace", info);

                    synchronized (aggregated) {
                        if (!aggregated.containsKey("redis_version")) {
                            aggregated.put("redis_version", serverInfo.getOrDefault("redis_version", ""));
                        }
                        long nodeUptime = Long.parseLong(serverInfo.getOrDefault("uptime_in_seconds", "0"));
                        aggregated.merge("uptime_in_seconds", String.valueOf(nodeUptime),
                                (a, b) -> String.valueOf(Math.max(Long.parseLong(a), Long.parseLong(b))));
                        long nodeClients = Long.parseLong(clientsInfo.getOrDefault("connected_clients", "0"));
                        aggregated.merge("connected_clients", String.valueOf(nodeClients),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeMem = Long.parseLong(memoryInfo.getOrDefault("used_memory", "0"));
                        aggregated.merge("used_memory", String.valueOf(nodeMem),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeRss = Long.parseLong(memoryInfo.getOrDefault("used_memory_rss", "0"));
                        aggregated.merge("used_memory_rss", String.valueOf(nodeRss),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        double nodeFrag = Double.parseDouble(memoryInfo.getOrDefault("mem_fragmentation_ratio", "0"));
                        aggregated.merge("mem_fragmentation_ratio", String.valueOf(nodeFrag),
                                (a, b) -> String.valueOf(Math.max(Double.parseDouble(a), Double.parseDouble(b))));
                        long nodeOps = Long.parseLong(statsInfo.getOrDefault("instantaneous_ops_per_sec", "0"));
                        aggregated.merge("instantaneous_ops_per_sec", String.valueOf(nodeOps),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeRejected = Long.parseLong(statsInfo.getOrDefault("rejected_connections", "0"));
                        aggregated.merge("rejected_connections", String.valueOf(nodeRejected),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeBlocked = Long.parseLong(clientsInfo.getOrDefault("blocked_clients", "0"));
                        aggregated.merge("blocked_clients", String.valueOf(nodeBlocked),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodePubsub = Long.parseLong(clientsInfo.getOrDefault("pubsub_channels", "0"));
                        aggregated.merge("pubsub_channels", String.valueOf(nodePubsub),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeHits = Long.parseLong(statsInfo.getOrDefault("keyspace_hits", "0"));
                        aggregated.merge("keyspace_hits", String.valueOf(nodeHits),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeMisses = Long.parseLong(statsInfo.getOrDefault("keyspace_misses", "0"));
                        aggregated.merge("keyspace_misses", String.valueOf(nodeMisses),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeExpired = Long.parseLong(statsInfo.getOrDefault("expired_keys", "0"));
                        aggregated.merge("expired_keys", String.valueOf(nodeExpired),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        long nodeEvicted = Long.parseLong(statsInfo.getOrDefault("evicted_keys", "0"));
                        aggregated.merge("evicted_keys", String.valueOf(nodeEvicted),
                                (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                        for (String val : keyspaceInfo.values()) {
                            for (String part : val.split(",")) {
                                if (part.startsWith("keys=")) {
                                    aggregated.merge("total_keys", part.substring(5),
                                            (a, b) -> String.valueOf(Long.parseLong(a) + Long.parseLong(b)));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            });

            long memBytes = aggregated.containsKey("used_memory") ? Long.parseLong(aggregated.get("used_memory")) : 0;
            aggregated.put("used_memory_human", formatBytes(memBytes));

            long totalHits = aggregated.containsKey("keyspace_hits") ? Long.parseLong(aggregated.get("keyspace_hits")) : 0;
            long totalMisses = aggregated.containsKey("keyspace_misses") ? Long.parseLong(aggregated.get("keyspace_misses")) : 0;
            long totalOps = totalHits + totalMisses;
            aggregated.put("hit_rate", totalOps > 0 ? String.format("%.2f", totalHits * 100.0 / totalOps) : "0");

            aggregated.putIfAbsent("redis_version", "");
            aggregated.putIfAbsent("uptime_in_seconds", "0");
            aggregated.putIfAbsent("connected_clients", "0");
            aggregated.putIfAbsent("used_memory_human", "-");
            aggregated.putIfAbsent("used_memory_rss", "0");
            aggregated.putIfAbsent("mem_fragmentation_ratio", "0");
            aggregated.putIfAbsent("total_keys", "0");
            aggregated.putIfAbsent("instantaneous_ops_per_sec", "0");
            aggregated.putIfAbsent("rejected_connections", "0");
            aggregated.putIfAbsent("blocked_clients", "0");
            aggregated.putIfAbsent("pubsub_channels", "0");
            aggregated.putIfAbsent("expired_keys", "0");
            aggregated.putIfAbsent("evicted_keys", "0");
            aggregated.putIfAbsent("keyspace_hits", "0");
            aggregated.putIfAbsent("keyspace_misses", "0");
        } else {
            return execute(jedis -> {
                String info = jedis.info();
                Map<String, String> serverInfo = parseInfo("Server", info);
                Map<String, String> clientsInfo = parseInfo("Clients", info);
                Map<String, String> memoryInfo = parseInfo("Memory", info);
                Map<String, String> statsInfo = parseInfo("Stats", info);
                Map<String, String> keyspaceInfo = parseInfo("Keyspace", info);
                long totalKeys = 0;
                for (String val : keyspaceInfo.values()) {
                    for (String part : val.split(",")) {
                        if (part.startsWith("keys=")) totalKeys += Long.parseLong(part.substring(5));
                    }
                }
                aggregated.put("redis_version", serverInfo.getOrDefault("redis_version", ""));
                aggregated.put("uptime_in_seconds", serverInfo.getOrDefault("uptime_in_seconds", "0"));
                aggregated.put("connected_clients", clientsInfo.getOrDefault("connected_clients", "0"));
                aggregated.put("used_memory_human", memoryInfo.getOrDefault("used_memory_human", ""));
                aggregated.put("used_memory_rss", memoryInfo.getOrDefault("used_memory_rss", "0"));
                aggregated.put("mem_fragmentation_ratio", memoryInfo.getOrDefault("mem_fragmentation_ratio", "0"));
                aggregated.put("total_keys", String.valueOf(totalKeys));
                long hitVal = Long.parseLong(statsInfo.getOrDefault("keyspace_hits", "0"));
                long missVal = Long.parseLong(statsInfo.getOrDefault("keyspace_misses", "0"));
                long total = hitVal + missVal;
                aggregated.put("hit_rate", total > 0 ? String.format("%.2f", hitVal * 100.0 / total) : "0");
                aggregated.put("instantaneous_ops_per_sec", statsInfo.getOrDefault("instantaneous_ops_per_sec", "0"));
            aggregated.put("rejected_connections", statsInfo.getOrDefault("rejected_connections", "0"));
            aggregated.put("blocked_clients", clientsInfo.getOrDefault("blocked_clients", "0"));
            aggregated.put("pubsub_channels", clientsInfo.getOrDefault("pubsub_channels", "0"));
            aggregated.put("expired_keys", statsInfo.getOrDefault("expired_keys", "0"));
            aggregated.put("evicted_keys", statsInfo.getOrDefault("evicted_keys", "0"));
            aggregated.put("keyspace_hits", String.valueOf(hitVal));
            aggregated.put("keyspace_misses", String.valueOf(missVal));
                return aggregated;
            });
        }
        return aggregated;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
