package qingzhou.app.redis.service;

import qingzhou.api.ChartType;
import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.action.Page;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.util.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Model(code = "memoryAnalysis", icon = "PieChart", menu = "redis-monitor", order = 6,
        name = {"内存分析", "en:Memory Analysis"},
        info = {"按 Key 前缀、大 Key、数据类型多维度分析内存分布，帮助定位内存热点。",
                "en:Analyze memory distribution by key prefix, big keys, and data types to identify memory hotspots."})
public class MemoryAnalysis extends RedisModelBase implements Page, qingzhou.api.action.Show {

    private static final int MAX_SAMPLE = 10000;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final int BIG_KEY_TOP_N = 50;

    private static volatile Map<String, long[]> cachedPrefixStats;
    private static volatile Map<String, long[]> cachedTypeStats;
    private static volatile long cachedTimestamp = 0;
    private static volatile String cachedInstanceName;

    public static void clearCache() {
        cachedPrefixStats = null;
        cachedTypeStats = null;
        cachedTimestamp = 0;
        cachedInstanceName = null;
    }

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"ID", "en:ID"},
            info = {"分析项唯一标识", "en:Unique analysis item ID"})
    public String id;

    @ModelField(search = true, add = false, show = false, update = false,
            options = {"prefix", "bigkey", "typeStats"},
            input_type = InputType.select,
            name = {"分析维度", "en:Analysis Type"},
            info = {"选择内存分析维度：前缀统计 / 大 Key / 类型分布", "en:Select analysis dimension: prefix / big key / type stats"})
    public String analysisType;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"排名", "en:Rank"},
            info = {"排名序号", "en:Rank number"})
    public String rank;

    @ModelField(list = true, show = true, readonly = true, search = true,
            name = {"Key 前缀", "en:Key Prefix"},
            info = {"Key 的前缀（按分隔符 : 截取）", "en:Key prefix (split by colon delimiter)"})
    public String keyPrefix;

    @ModelField(list = true, show = true, readonly = true,
            name = {"Key 名称", "en:Key Name"},
            info = {"Redis Key 名称", "en:Redis key name"})
    public String keyName;

    @ModelField(list = true, show = true, readonly = true, search = true,
            name = {"数据类型", "en:Type"},
            info = {"Key 的数据类型", "en:Data type of the key"})
    public String keyType;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            chart_type = ChartType.pie,
            chart_title = "KeyCount",
            name = {"Key 数量", "en:Key Count"},
            info = {"该项下的 Key 数量", "en:Number of keys"})
    public String keyCount;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"数量占比(%)", "en:Count %"},
            info = {"占总分析 Key 数量的百分比", "en:Percentage of total analyzed keys"})
    public String countRatio;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            chart_type = ChartType.pie,
            chart_title = "MemoryDist",
            name = {"内存占比(%)", "en:Memory %"},
            info = {"占总分析内存的百分比", "en:Percentage of total analyzed memory"})
    public String memoryPercent;

    @ModelField(list = true, show = true, readonly = true,
            name = {"内存占用", "en:Memory Usage"},
            info = {"内存占用（字节）", "en:Memory usage in bytes"})
    public String memoryUsage;

    @ModelField(list = true, show = true, readonly = true,
            name = {"内部编码", "en:Encoding"},
            info = {"Redis 内部编码", "en:Redis internal encoding"})
    public String encoding;

    @Override
    public List<String[]> page(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) throws Exception {
        String type = query != null ? query.get("analysisType") : null;
        if (type == null || type.isEmpty()) {
            type = "prefix";
        }

        List<String[]> rows;
        switch (type) {
            case "bigkey":
                rows = listBigKeys();
                break;
            case "typeStats":
                rows = listTypeStats();
                break;
            case "prefix":
            default:
                rows = listPrefixStats(query);
                break;
        }

        int start = (pageNum - 1) * pageSize;
        if (start >= rows.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, rows.size());
        return rows.subList(start, end);
    }

    private List<String[]> listPrefixStats(Map<String, String> query) {
        Map<String, long[]> prefixStats = getOrRefreshCache();

        long totalMemory = 0;
        long totalCount = 0;
        for (long[] stats : prefixStats.values()) {
            totalMemory += stats[1];
            totalCount += stats[0];
        }

        String searchPrefix = query != null ? query.get("keyPrefix") : null;
        Map<String, long[]> filtered = new LinkedHashMap<>(prefixStats);
        if (searchPrefix != null && !searchPrefix.isEmpty()) {
            filtered.entrySet().removeIf(e -> !e.getKey().toLowerCase().contains(searchPrefix.toLowerCase()));
        }

        List<Map.Entry<String, long[]>> sorted = new ArrayList<>(filtered.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));

        List<String[]> rows = new ArrayList<>();
        int rankNum = 1;
        for (Map.Entry<String, long[]> entry : sorted) {
            String prefix = entry.getKey();
            long count = entry.getValue()[0];
            long memBytes = entry.getValue()[1];
            String memPercent = totalMemory > 0 ? String.format("%.2f", memBytes * 100.0 / totalMemory) : "0";
            String countPercent = totalCount > 0 ? String.format("%.2f", count * 100.0 / totalCount) : "0";
            rows.add(new String[]{
                    "prefix:" + prefix,
                    String.valueOf(rankNum++),
                    prefix,
                    "",
                    "",
                    String.valueOf(count),
                    countPercent,
                    memPercent,
                    RedisUtil.formatBytes(memBytes),
                    ""
            });
        }
        return rows;
    }

    private List<String[]> listBigKeys() {
        List<Map.Entry<String, Long>> bigKeys = getRedisUtil().findBigKeys(BIG_KEY_TOP_N);
        if (bigKeys == null || bigKeys.isEmpty()) {
            return new ArrayList<>();
        }

        List<String[]> rows = new ArrayList<>();
        int rankNum = 1;
        for (Map.Entry<String, Long> entry : bigKeys) {
            if (rankNum > BIG_KEY_TOP_N) break;
            String key = entry.getKey();
            long memBytes = entry.getValue();
            String type = getKeyType(key);
            String enc = getEncoding(key);
            rows.add(new String[]{
                    "bigkey:" + key,
                    String.valueOf(rankNum++),
                    "",
                    key,
                    type,
                    "",
                    "",
                    "",
                    RedisUtil.formatBytes(memBytes),
                    enc
            });
        }
        return rows;
    }

    private List<String[]> listTypeStats() {
        Map<String, long[]> typeStats = getTypeStatsCache();
        if (typeStats == null || typeStats.isEmpty()) {
            return new ArrayList<>();
        }

        long totalMemory = 0;
        long totalCount = 0;
        for (long[] stats : typeStats.values()) {
            totalMemory += stats[1];
            totalCount += stats[0];
        }

        List<Map.Entry<String, long[]>> sorted = new ArrayList<>(typeStats.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));

        List<String[]> rows = new ArrayList<>();
        int rankNum = 1;
        for (Map.Entry<String, long[]> entry : sorted) {
            String type = entry.getKey();
            long count = entry.getValue()[0];
            long memBytes = entry.getValue()[1];
            String memPercent = totalMemory > 0 ? String.format("%.2f", memBytes * 100.0 / totalMemory) : "0";
            String countPercent = totalCount > 0 ? String.format("%.2f", count * 100.0 / totalCount) : "0";
            rows.add(new String[]{
                    "typeStats:" + type,
                    String.valueOf(rankNum++),
                    "",
                    "",
                    type,
                    String.valueOf(count),
                    countPercent,
                    memPercent,
                    RedisUtil.formatBytes(memBytes),
                    ""
            });
        }
        return rows;
    }

    private String getKeyType(String key) {
        try {
            if (getRedisUtil().isCluster()) {
                return getRedisUtil().executeWithCluster(jedisCluster -> jedisCluster.type(key));
            }
            return getRedisUtil().execute(jedis -> jedis.type(key));
        } catch (Exception e) {
            return "";
        }
    }

    private String getEncoding(String key) {
        try {
            if (getRedisUtil().isCluster()) {
                return "";
            }
            return getRedisUtil().execute(jedis -> {
                String enc = jedis.objectEncoding(key);
                return enc != null ? enc : "";
            });
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, long[]> getOrRefreshCache() {
        long now = System.currentTimeMillis();
        String currentInstance = qingzhou.app.redis.RedisApp.getCurrentInstanceName();

        if (cachedPrefixStats != null && (now - cachedTimestamp) < CACHE_TTL_MS
                && currentInstance != null && currentInstance.equals(cachedInstanceName)) {
            return cachedPrefixStats;
        }

        Map<String, long[]> prefixStats = new ConcurrentHashMap<>();
        Map<String, long[]> typeStats = new ConcurrentHashMap<>();

        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectStats(jedis, prefixStats, typeStats));
        } else {
            getRedisUtil().execute(jedis -> {
                collectStats(jedis, prefixStats, typeStats);
                return null;
            });
        }

        cachedPrefixStats = prefixStats;
        cachedTypeStats = typeStats;
        cachedTimestamp = now;
        cachedInstanceName = currentInstance;
        return prefixStats;
    }

    private Map<String, long[]> getTypeStatsCache() {
        getOrRefreshCache();
        return cachedTypeStats;
    }

    private void collectStats(Jedis jedis, Map<String, long[]> prefixStats, Map<String, long[]> typeStats) {
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().count(500);
        int sampled = 0;

        List<String> batchKeys = new ArrayList<>();
        String currentCursor = cursor;

        do {
            ScanResult<String> result = jedis.scan(currentCursor, params);
            for (String key : result.getResult()) {
                batchKeys.add(key);
                sampled++;
                if (sampled >= MAX_SAMPLE) break;
            }
            currentCursor = result.getCursor();
        } while (!currentCursor.equals(ScanParams.SCAN_POINTER_START) && sampled < MAX_SAMPLE);

        if (!batchKeys.isEmpty()) {
            Pipeline pipeline = jedis.pipelined();
            for (String key : batchKeys) {
                pipeline.memoryUsage(key);
                pipeline.type(key);
            }
            List<Object> results = pipeline.syncAndReturnAll();

            for (int i = 0; i < batchKeys.size(); i++) {
                String key = batchKeys.get(i);
                String prefix = extractPrefix(key);

                Object memObj = results.get(i * 2);
                long memVal = 0;
                if (memObj instanceof Long) {
                    memVal = (Long) memObj;
                }

                Object typeObj = results.get(i * 2 + 1);
                String type = String.valueOf(typeObj);

                final long memBytes = memVal;

                prefixStats.compute(prefix, (k, v) -> {
                    if (v == null) return new long[]{1, memBytes};
                    v[0]++;
                    v[1] += memBytes;
                    return v;
                });

                typeStats.compute(type, (k, v) -> {
                    if (v == null) return new long[]{1, memBytes};
                    v[0]++;
                    v[1] += memBytes;
                    return v;
                });
            }
        }
    }

    private String extractPrefix(String key) {
        int firstColon = key.indexOf(':');
        if (firstColon < 0) return key;
        int secondColon = key.indexOf(':', firstColon + 1);
        if (secondColon < 0) return key.substring(0, firstColon);
        return key.substring(0, secondColon);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String type = query != null ? query.get("analysisType") : null;
        if (type == null || type.isEmpty()) {
            type = "prefix";
        }

        switch (type) {
            case "bigkey":
                return Math.min(getRedisUtil().findBigKeys(BIG_KEY_TOP_N).size(), BIG_KEY_TOP_N);
            case "typeStats":
                Map<String, long[]> typeStats = getTypeStatsCache();
                return typeStats != null ? typeStats.size() : 0;
            case "prefix":
            default:
                Map<String, long[]> stats = getOrRefreshCache();
                String searchPrefix = query != null ? query.get("keyPrefix") : null;
                if (searchPrefix != null && !searchPrefix.isEmpty()) {
                    int count = 0;
                    for (String prefix : stats.keySet()) {
                        if (prefix.toLowerCase().contains(searchPrefix.toLowerCase())) count++;
                    }
                    return count;
                }
                return stats.size();
        }
    }

    @Override
    public boolean contains(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (id.startsWith("prefix:")) {
            return getOrRefreshCache().containsKey(id.substring(7));
        }
        if (id.startsWith("typeStats:")) {
            Map<String, long[]> typeStats = getTypeStatsCache();
            return typeStats != null && typeStats.containsKey(id.substring(10));
        }
        if (id.startsWith("bigkey:")) {
            String key = id.substring(7);
            for (Map.Entry<String, Long> entry : getRedisUtil().findBigKeys(BIG_KEY_TOP_N)) {
                if (entry.getKey().equals(key)) return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", id);
        if (id == null || id.isEmpty()) {
            return data;
        }

        if (id.startsWith("prefix:")) {
            String prefix = id.substring(7);
            long[] stats = getOrRefreshCache().get(prefix);
            if (stats != null) {
                data.put("analysisType", "prefix");
                data.put("keyPrefix", prefix);
                data.put("keyCount", String.valueOf(stats[0]));
                data.put("memoryUsage", RedisUtil.formatBytes(stats[1]));
            }
        } else if (id.startsWith("typeStats:")) {
            String type = id.substring(10);
            Map<String, long[]> typeStats = getTypeStatsCache();
            if (typeStats != null) {
                long[] stats = typeStats.get(type);
                if (stats != null) {
                    data.put("analysisType", "typeStats");
                    data.put("keyType", type);
                    data.put("keyCount", String.valueOf(stats[0]));
                    data.put("memoryUsage", RedisUtil.formatBytes(stats[1]));
                }
            }
        } else if (id.startsWith("bigkey:")) {
            String key = id.substring(7);
            for (Map.Entry<String, Long> entry : getRedisUtil().findBigKeys(BIG_KEY_TOP_N)) {
                if (entry.getKey().equals(key)) {
                    data.put("analysisType", "bigkey");
                    data.put("keyName", key);
                    data.put("keyType", getKeyType(key));
                    data.put("memoryUsage", RedisUtil.formatBytes(entry.getValue()));
                    data.put("encoding", getEncoding(key));
                    break;
                }
            }
        }
        return data;
    }

    @ModelAction(name = {"刷新缓存", "en:Refresh Cache"},
            info = {"清除采样缓存并重新分析", "en:Clear sampling cache and re-analyze"},
            list_head = true)
    public void refreshCache(Request request) {
        clearCache();
    }
}
