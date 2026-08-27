package qingzhou.app.redis.service;

import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.action.*;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.store.model.AuditEntry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@Model(code = "keyBrowser", icon = "Search", menu = "redis", order = 1,
        name = {"Key 管理", "en:Key Browser"},
        info = {"浏览和管理 Redis Key 数据，支持新增、查看、修改、删除操作。",
                "en:Browse and manage Redis keys, supporting add, view, edit, and delete operations."})
public class KeyBrowser extends RedisModelBase implements Page, Add, Delete, Show, Update {

    private static final int MAX_SCAN_COUNT = 10000;

    @ModelField(id = true, add = true, list = true, show = true, search = true, update = false, required = true,
            name = {"Key 名称", "en:Key Name"},
            info = {"Redis 中存储的 Key 名称", "en:Key name stored in Redis"})
    public String key;

    @ModelField(add = true, list = true, update = false,
            input_type = InputType.select,
            options = {"string", "list", "set", "zset", "hash"},
            name = {"数据类型", "en:Type"},
            info = {"Key 的数据类型：string/list/set/zset/hash", "en:Data type of the key"})
    public String type = "string";

    @ModelField(add = true, input_type = InputType.textarea,
            name = {"值", "en:Value"},
            info = {"Key 的值内容。String 填写单值；List/Set 每行一个元素；Hash 每行 key:value；ZSet 每行 score:member",
                    "en:Value content. String: single value; List/Set: one per line; Hash: key:value; ZSet: score:member"})
    public String value;

    @ModelField(list = true, numeric = true,
            name = {"TTL（秒）", "en:TTL(sec)"},
            info = {"剩余存活时间（秒），-1 永不过期，-2 已过期；新增/编辑时输入0表示永不过期",
                    "en:Time to live in seconds, -1 no expiry; set 0 for no expiry when adding/editing"})
    public String ttl;

    @ModelField(list = true, show = true, numeric = true, readonly = true,
            name = {"大小", "en:Size"},
            info = {"集合类型的元素数量", "en:Number of elements for collection types"})
    public String size;

    @ModelField(show = true, readonly = true,
            name = {"内部编码", "en:Encoding"},
            info = {"Redis 内部使用的编码格式", "en:Internal encoding format used by Redis"})
    public String encoding;

    @ModelField(search = true, add = false, show = false, update = false,
            options = {"all", "string", "list", "set", "zset", "hash", "stream"},
            name = {"类型筛选", "en:Type Filter"},
            info = {"按数据类型筛选", "en:Filter by data type"})
    public String typeFilter;

    @ModelField(search = true, add = false, show = false, update = false,
            options = {"all", "active", "expired", "persistent"},
            name = {"过期筛选", "en:TTL Status"},
            info = {"按TTL状态筛选：active-有TTL, expired-已过期, persistent-永不过期",
                    "en:Filter by TTL status: active-has TTL, expired-expired, persistent-no expiry"})
    public String ttlFilter;

    @Override
    public java.util.List<String[]> page(int pageNum, int pageSize,
                                         Map<String, String> query, String[] listFields) {
        String rawPattern = query != null ? query.get("key") : null;
        final String pattern = (rawPattern == null || rawPattern.isEmpty()) ? "*" : "*" + rawPattern + "*";
        final String typeFilterVal = query != null ? query.get("typeFilter") : null;
        final String ttlFilterVal = query != null ? query.get("ttlFilter") : null;

        Set<String> allKeys = new LinkedHashSet<>();
        if (getRedisUtil().isCluster()) {
            int perNodeLimit = Math.max(1000, MAX_SCAN_COUNT / getRedisUtil().executeWithCluster(JedisCluster::getClusterNodes).size());
            final int limit = perNodeLimit;
            getRedisUtil().executeOnAllClusterNodes(jedis -> scanKeys(jedis, pattern, allKeys, limit));
        } else {
            getRedisUtil().execute(jedis -> {
                scanKeys(jedis, pattern, allKeys, MAX_SCAN_COUNT);
                return null;
            });
        }

        String[] keyArray = allKeys.toArray(new String[0]);

        if (getRedisUtil().isCluster()) {
            return getRedisUtil().executeWithCluster(jedisCluster -> {
                java.util.List<KeyInfo> infos = new ArrayList<>();
                for (String k : keyArray) {
                    String t = jedisCluster.type(k);
                    long ttlVal = jedisCluster.ttl(k);
                    if (!matchesTypeFilter(t, typeFilterVal)) continue;
                    if (!matchesTtlFilter(ttlVal, ttlFilterVal)) continue;
                    long sz = getSizeByType(jedisCluster, k, t);
                    infos.add(new KeyInfo(k, t, ttlVal, sz));
                }
                int start = (pageNum - 1) * pageSize;
                if (start >= infos.size()) return new ArrayList<>();
                int end = Math.min(start + pageSize, infos.size());
                java.util.List<String[]> rows = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    KeyInfo info = infos.get(i);
                    rows.add(new String[]{info.key, info.type, String.valueOf(info.ttl), String.valueOf(info.size)});
                }
                return rows;
            });
        } else {
            return getRedisUtil().execute(jedis -> {
                Pipeline pipeline = jedis.pipelined();
                for (String k : keyArray) {
                    pipeline.type(k);
                    pipeline.ttl(k);
                }
                List<Object> results = pipeline.syncAndReturnAll();

                java.util.List<KeyInfo> filteredKeys = new ArrayList<>();
                for (int i = 0; i < keyArray.length; i++) {
                    String t = String.valueOf(results.get(i * 2));
                    long ttlVal = (Long) results.get(i * 2 + 1);
                    if (!matchesTypeFilter(t, typeFilterVal)) continue;
                    if (!matchesTtlFilter(ttlVal, ttlFilterVal)) continue;
                    filteredKeys.add(new KeyInfo(keyArray[i], t, ttlVal, 0));
                }

                int start = (pageNum - 1) * pageSize;
                if (start >= filteredKeys.size()) return new ArrayList<>();
                int end = Math.min(start + pageSize, filteredKeys.size());

                Pipeline sizePipeline = jedis.pipelined();
                for (int i = start; i < end; i++) {
                    KeyInfo info = filteredKeys.get(i);
                    switch (info.type) {
                        case "list":
                            sizePipeline.llen(info.key);
                            break;
                        case "set":
                            sizePipeline.scard(info.key);
                            break;
                        case "zset":
                            sizePipeline.zcard(info.key);
                            break;
                        case "hash":
                            sizePipeline.hlen(info.key);
                            break;
                        case "stream":
                            sizePipeline.xlen(info.key);
                            break;
                        default:
                            sizePipeline.strlen(info.key);
                            break;
                    }
                }
                List<Object> sizeResults = sizePipeline.syncAndReturnAll();

                java.util.List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < end - start; i++) {
                    KeyInfo info = filteredKeys.get(start + i);
                    long sz = 0;
                    if (!"none".equals(info.type) && !"string".equals(info.type)) {
                        Object sizeObj = sizeResults.get(i);
                        if (sizeObj instanceof Long) sz = (Long) sizeObj;
                    }
                    rows.add(new String[]{info.key, info.type, String.valueOf(info.ttl), String.valueOf(sz)});
                }
                return rows;
            });
        }
    }

    private static class KeyInfo {
        final String key;
        final String type;
        final long ttl;
        long size;

        KeyInfo(String key, String type, long ttl, long size) {
            this.key = key;
            this.type = type;
            this.ttl = ttl;
            this.size = size;
        }
    }

    private boolean matchesTypeFilter(String actualType, String typeFilter) {
        if (typeFilter == null || typeFilter.isEmpty() || "all".equals(typeFilter)) return true;
        return typeFilter.equals(actualType);
    }

    private boolean matchesTtlFilter(long ttl, String ttlFilter) {
        if (ttlFilter == null || ttlFilter.isEmpty() || "all".equals(ttlFilter)) return true;
        switch (ttlFilter) {
            case "active":
                return ttl > 0;
            case "expired":
                return ttl == -2;
            case "persistent":
                return ttl == -1;
            default:
                return true;
        }
    }

    private long getSizeByType(JedisCluster jedisCluster, String key, String type) {
        switch (type) {
            case "list":
                return jedisCluster.llen(key);
            case "set":
                return jedisCluster.scard(key);
            case "zset":
                return jedisCluster.zcard(key);
            case "hash":
                return jedisCluster.hlen(key);
            case "stream":
                return jedisCluster.xlen(key);
            default:
                return 0;
        }
    }

    private void scanKeys(Jedis jedis, String pattern, Set<String> collector, int maxCount) {
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match(pattern).count(500);
        int scanned = 0;
        do {
            ScanResult<String> result = jedis.scan(cursor, params);
            collector.addAll(result.getResult());
            cursor = result.getCursor();
            scanned += result.getResult().size();
            if (scanned >= maxCount) break;
        } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String pattern = query != null ? query.get("key") : null;
        if (pattern == null || pattern.isEmpty()) pattern = "*";
        else pattern = "*" + pattern + "*";
        String finalPattern = pattern;

        int[] count = {0};
        if (getRedisUtil().isCluster()) {
            int perNodeLimit = Math.max(1000, MAX_SCAN_COUNT / getRedisUtil().executeWithCluster(JedisCluster::getClusterNodes).size());
            final int limit = perNodeLimit;
            getRedisUtil().executeOnAllClusterNodes(jedis -> {
                String cursor = ScanParams.SCAN_POINTER_START;
                ScanParams params = new ScanParams().match(finalPattern).count(500);
                do {
                    ScanResult<String> result = jedis.scan(cursor, params);
                    count[0] += result.getResult().size();
                    cursor = result.getCursor();
                    if (count[0] >= MAX_SCAN_COUNT) break;
                } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
            });
        } else {
            getRedisUtil().execute(jedis -> {
                String cursor = ScanParams.SCAN_POINTER_START;
                ScanParams params = new ScanParams().match(finalPattern).count(500);
                do {
                    ScanResult<String> result = jedis.scan(cursor, params);
                    count[0] += result.getResult().size();
                    cursor = result.getCursor();
                    if (count[0] >= MAX_SCAN_COUNT) break;
                } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
                return null;
            });
        }
        return count[0];
    }

    @Override
    public boolean contains(String id) {
        if (getRedisUtil().isCluster()) {
            return getRedisUtil().executeWithCluster(jedisCluster -> jedisCluster.exists(id));
        }
        return getRedisUtil().execute(jedis -> jedis.exists(id));
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        if (!checkWriteAllowed(getCurrentRequest())) return;
        String k = data.get("key");
        if (k == null || k.trim().isEmpty()) {
            throw new Exception("Key 名称不能为空");
        }
        final String typeVal = data.getOrDefault("type", "string");
        final String v = data.get("value");
        String ttlStr = data.get("ttl");
        final int ttlSec = (ttlStr != null && !ttlStr.isEmpty()) ? Integer.parseInt(ttlStr) : 0;

        if (getRedisUtil().isCluster()) {
            if (getRedisUtil().executeWithCluster(jedisCluster -> jedisCluster.exists(k))) {
                throw new Exception("Key 已存在：" + k);
            }
        } else {
            if (getRedisUtil().execute(jedis -> jedis.exists(k))) {
                throw new Exception("Key 已存在：" + k);
            }
        }

        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeWithCluster(jedisCluster -> {
                addKey(jedisCluster, k, typeVal, v, ttlSec);
                return null;
            });
        } else {
            getRedisUtil().execute(jedis -> {
                addKeyStandalone(jedis, k, typeVal, v, ttlSec);
                return null;
            });
        }
        logAudit("add", k, "type=" + typeVal + ", ttl=" + ttlSec);
    }

    private void addKeyStandalone(Jedis jedis, String k, String typeVal, String v, int ttlSec) {
        switch (typeVal) {
            case "string":
                if (ttlSec > 0) jedis.setex(k, ttlSec, v != null ? v : "");
                else jedis.set(k, v != null ? v : "");
                break;
            case "list":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedis.rpush(k, item.trim());
                    }
                }
                if (ttlSec > 0) jedis.expire(k, ttlSec);
                break;
            case "set":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedis.sadd(k, item.trim());
                    }
                }
                if (ttlSec > 0) jedis.expire(k, ttlSec);
                break;
            case "zset":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            double score = Double.parseDouble(item.substring(0, colonIdx).trim());
                            String member = item.substring(colonIdx + 1).trim();
                            jedis.zadd(k, score, member);
                        }
                    }
                }
                if (ttlSec > 0) jedis.expire(k, ttlSec);
                break;
            case "hash":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            String field = item.substring(0, colonIdx).trim();
                            String fieldVal = item.substring(colonIdx + 1).trim();
                            jedis.hset(k, field, fieldVal);
                        }
                    }
                }
                if (ttlSec > 0) jedis.expire(k, ttlSec);
                break;
            default:
                if (ttlSec > 0) jedis.setex(k, ttlSec, v != null ? v : "");
                else jedis.set(k, v != null ? v : "");
        }
    }

    private void addKey(JedisCluster jedisCluster, String k, String typeVal, String v, int ttlSec) {
        switch (typeVal) {
            case "string":
                if (ttlSec > 0) jedisCluster.setex(k, ttlSec, v != null ? v : "");
                else jedisCluster.set(k, v != null ? v : "");
                break;
            case "list":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedisCluster.rpush(k, item.trim());
                    }
                }
                if (ttlSec > 0) jedisCluster.expire(k, ttlSec);
                break;
            case "set":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedisCluster.sadd(k, item.trim());
                    }
                }
                if (ttlSec > 0) jedisCluster.expire(k, ttlSec);
                break;
            case "zset":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            double score = Double.parseDouble(item.substring(0, colonIdx).trim());
                            String member = item.substring(colonIdx + 1).trim();
                            jedisCluster.zadd(k, score, member);
                        }
                    }
                }
                if (ttlSec > 0) jedisCluster.expire(k, ttlSec);
                break;
            case "hash":
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            String field = item.substring(0, colonIdx).trim();
                            String fieldVal = item.substring(colonIdx + 1).trim();
                            jedisCluster.hset(k, field, fieldVal);
                        }
                    }
                }
                if (ttlSec > 0) jedisCluster.expire(k, ttlSec);
                break;
            default:
                if (ttlSec > 0) jedisCluster.setex(k, ttlSec, v != null ? v : "");
                else jedisCluster.set(k, v != null ? v : "");
        }
    }

    

    @Override
    public Map<String, String> show(String id) {
        if (getRedisUtil().isCluster()) {
            return getRedisUtil().executeWithCluster(jedisCluster -> showKey(jedisCluster, id));
        }
        return getRedisUtil().execute(jedis -> showKeyStandalone(jedis, id));
    }

    private Map<String, String> showKeyStandalone(Jedis jedis, String id) {
        Map<String, String> data = new LinkedHashMap<>();
        String t = jedis.type(id);
        long ttlVal = jedis.ttl(id);
        String enc = jedis.objectEncoding(id);
        data.put("key", id);
        data.put("type", t);
        data.put("ttl", String.valueOf(ttlVal));
        data.put("encoding", enc != null ? enc : "-");
        fillValue(data, jedis, id, t);
        return data;
    }

    private Map<String, String> showKey(JedisCluster jedisCluster, String id) {
        Map<String, String> data = new LinkedHashMap<>();
        String t = jedisCluster.type(id);
        long ttlVal = jedisCluster.ttl(id);
        data.put("key", id);
        data.put("type", t);
        data.put("ttl", String.valueOf(ttlVal));
        data.put("encoding", "-"); 
        fillValueCluster(data, jedisCluster, id, t);
        return data;
    }

    private void fillValue(Map<String, String> data, Jedis jedis, String id, String t) {
        StringBuilder valueStr = new StringBuilder();
        switch (t) {
            case "string":
                valueStr.append(jedis.get(id));
                break;
            case "list": {
                long len = jedis.llen(id);
                data.put("size", String.valueOf(len));
                long limit = Math.min(len, 100);
                for (long i = 0; i < limit; i++) {
                    valueStr.append(jedis.lindex(id, i)).append("\n");
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "set": {
                long len = jedis.scard(id);
                data.put("size", String.valueOf(len));
                java.util.Set<String> members = jedis.smembers(id);
                int idx = 0;
                if (members != null) {
                    for (String m : members) {
                        if (idx++ >= 100) break;
                        valueStr.append(m).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "zset": {
                long len = jedis.zcard(id);
                data.put("size", String.valueOf(len));
                java.util.List<redis.clients.jedis.resps.Tuple> tuples = jedis.zrangeWithScores(id, 0, 99);
                int idx = 0;
                if (tuples != null) {
                    for (redis.clients.jedis.resps.Tuple tuple : tuples) {
                        if (idx++ >= 100) break;
                        valueStr.append(tuple.getScore()).append(":").append(tuple.getElement()).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "hash": {
                long len = jedis.hlen(id);
                data.put("size", String.valueOf(len));
                Map<String, String> fields = jedis.hgetAll(id);
                int count = 0;
                if (fields != null) {
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        if (count++ >= 100) break;
                        valueStr.append(e.getKey()).append(":").append(e.getValue()).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "stream": {
                long len = jedis.xlen(id);
                data.put("size", String.valueOf(len));
                valueStr.append("Stream length: ").append(len);
                break;
            }
            default:
                valueStr.append("Unsupported type: ").append(t);
        }
        data.put("value", valueStr.toString());
    }

    private void fillValueCluster(Map<String, String> data, JedisCluster jedisCluster, String id, String t) {
        StringBuilder valueStr = new StringBuilder();
        switch (t) {
            case "string":
                valueStr.append(jedisCluster.get(id));
                break;
            case "list": {
                long len = jedisCluster.llen(id);
                data.put("size", String.valueOf(len));
                long limit = Math.min(len, 100);
                for (long i = 0; i < limit; i++) {
                    valueStr.append(jedisCluster.lindex(id, i)).append("\n");
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "set": {
                long len = jedisCluster.scard(id);
                data.put("size", String.valueOf(len));
                java.util.Set<String> members = jedisCluster.smembers(id);
                int idx = 0;
                if (members != null) {
                    for (String m : members) {
                        if (idx++ >= 100) break;
                        valueStr.append(m).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "zset": {
                long len = jedisCluster.zcard(id);
                data.put("size", String.valueOf(len));
                java.util.List<redis.clients.jedis.resps.Tuple> tuples = jedisCluster.zrangeWithScores(id, 0, 99);
                int idx = 0;
                if (tuples != null) {
                    for (redis.clients.jedis.resps.Tuple tuple : tuples) {
                        if (idx++ >= 100) break;
                        valueStr.append(tuple.getScore()).append(":").append(tuple.getElement()).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "hash": {
                long len = jedisCluster.hlen(id);
                data.put("size", String.valueOf(len));
                Map<String, String> fields = jedisCluster.hgetAll(id);
                int count = 0;
                if (fields != null) {
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        if (count++ >= 100) break;
                        valueStr.append(e.getKey()).append(":").append(e.getValue()).append("\n");
                    }
                }
                if (len > 100) valueStr.append("...(截断，共").append(len).append("条)");
                break;
            }
            case "stream": {
                long len = jedisCluster.xlen(id);
                data.put("size", String.valueOf(len));
                valueStr.append("Stream length: ").append(len);
                break;
            }
            default:
                valueStr.append("Unsupported type: ").append(t);
        }
        data.put("value", valueStr.toString());
    }

    @Override
    public void update(String id, Map<String, String> data) {
        if (!checkWriteAllowed(getCurrentRequest())) return;
        String v = data.get("value");
        String ttlStr = data.get("ttl");

        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeWithCluster(jedisCluster -> {
                updateKey(jedisCluster, id, v, ttlStr);
                return null;
            });
        } else {
            getRedisUtil().execute(jedis -> {
                updateKeyStandalone(jedis, id, v, ttlStr);
                return null;
            });
        }
        logAudit("update", id, "updated ttl=" + ttlStr);
    }

    private void updateKeyStandalone(Jedis jedis, String id, String v, String ttlStr) {
        String currentType = jedis.type(id);
        switch (currentType) {
            case "string":
                if (v != null) jedis.set(id, v);
                break;
            case "list":
                
                jedis.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedis.rpush(id, item.trim());
                    }
                }
                break;
            case "set":
                
                jedis.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedis.sadd(id, item.trim());
                    }
                }
                break;
            case "zset":
                
                jedis.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            double score = Double.parseDouble(item.substring(0, colonIdx).trim());
                            String member = item.substring(colonIdx + 1).trim();
                            jedis.zadd(id, score, member);
                        }
                    }
                }
                break;
            case "hash":
                
                if (v != null) {
                    jedis.del(id);
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            jedis.hset(id, item.substring(0, colonIdx).trim(), item.substring(colonIdx + 1).trim());
                        }
                    }
                }
                break;
        }
        
        if (ttlStr != null && !ttlStr.isEmpty()) {
            long t = Long.parseLong(ttlStr);
            if (t > 0) jedis.expire(id, t);
            else if (t == -1) jedis.persist(id);
        }
    }

    private void updateKey(JedisCluster jedisCluster, String id, String v, String ttlStr) {
        String currentType = jedisCluster.type(id);
        switch (currentType) {
            case "string":
                if (v != null) jedisCluster.set(id, v);
                break;
            case "list":
                jedisCluster.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedisCluster.rpush(id, item.trim());
                    }
                }
                break;
            case "set":
                jedisCluster.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (!item.trim().isEmpty()) jedisCluster.sadd(id, item.trim());
                    }
                }
                break;
            case "zset":
                jedisCluster.del(id);
                if (v != null && !v.isEmpty()) {
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            double score = Double.parseDouble(item.substring(0, colonIdx).trim());
                            String member = item.substring(colonIdx + 1).trim();
                            jedisCluster.zadd(id, score, member);
                        }
                    }
                }
                break;
            case "hash":
                if (v != null) {
                    jedisCluster.del(id);
                    for (String item : v.split("\n")) {
                        if (item.trim().isEmpty()) continue;
                        int colonIdx = item.indexOf(':');
                        if (colonIdx > 0) {
                            jedisCluster.hset(id, item.substring(0, colonIdx).trim(), item.substring(colonIdx + 1).trim());
                        }
                    }
                }
                break;
        }
        
        if (ttlStr != null && !ttlStr.isEmpty()) {
            long t = Long.parseLong(ttlStr);
            if (t > 0) jedisCluster.expire(id, t);
            else if (t == -1) jedisCluster.persist(id);
        }
    }

    @Override
    public void delete(String id) throws Exception {
        checkDangerousAllowed();
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeWithCluster(jedisCluster -> jedisCluster.del(id));
        } else {
            getRedisUtil().execute(jedis -> jedis.del(id));
        }
        logAudit("delete", id, "deleted key");
    }

    private void logAudit(String operationType, String targetId, String detail) {
        AuditEntry audit = new AuditEntry();
        String operator = getCurrentRequest() != null
                ? getCurrentRequest().getParameter("operator") : null;
        audit.setOperator(operator != null && !operator.isEmpty() ? operator : "unknown");
        audit.setOperationType(operationType);
        audit.setTargetType("keyBrowser");
        audit.setTargetId(targetId);
        audit.setInstanceName(RedisApp.getCurrentInstanceName());
        audit.setEnvType(getCurrentEnvType());
        audit.setResult("成功");
        audit.setDetail(detail);
        RedisApp.getAuditStore().log(audit);
    }
}
