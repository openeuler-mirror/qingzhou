package qingzhou.app.redis.service;

import java.util.*;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Add;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.store.model.AuditEntry;

@Model(code = "serverConfig", icon = "Setting", menu = "redis", order = 2,
        name = {"配置查看", "en:Server Config"},
        info = {"查看 Redis 服务器的运行配置参数。", "en:View Redis server runtime configuration."})
public class ServerConfig extends RedisModelBase implements List, Show, Update, Add {

    
    private static final Set<String> READONLY_CONFIGS = new HashSet<>(Arrays.asList(
            "protected-mode", "bind", "port", "tls-port",
            "unixsocket", "unixsocketperm",
            "cluster-enabled", "cluster-config-file", "cluster-node-timeout",
            "slaveof", "replicaof", "masterauth",
            "tls-cert-file", "tls-key-file", "tls-ca-cert-file"
    ));

    @ModelField(id = true, list = true, show = true, readonly = true, search = true, add = true, update = false,
            name = {"参数名", "en:Parameter Name"},
            info = {"Redis 配置参数名称", "en:Redis configuration parameter name"})
    public String paramName;

    @ModelField(list = true, show = true, update = true, add = true, input_type = InputType.textarea,
            name = {"参数值", "en:Parameter Value"},
            info = {"Redis 配置参数值", "en:Redis configuration parameter value"})
    public String paramValue;

    @ModelField(show = true, readonly = true, add = false, update = false,
            name = {"只读", "en:Readonly"},
            info = {"该配置是否为只读（不可修改）", "en:Whether this config is readonly"})
    public String readonly;

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize,
                                         Map<String, String> query, String[] listFields) throws Exception {
        Map<String, String> rawMap;
        if (getRedisUtil().isCluster()) {
            rawMap = getRedisUtil().executeOnFirstClusterNode(jedis -> jedis.configGet("*"));
        } else {
            rawMap = getRedisUtil().execute(jedis -> jedis.configGet("*"));
        }

        if (rawMap == null) return new ArrayList<>();

        java.util.List<String[]> allRows = new ArrayList<>();
        for (Map.Entry<String, String> entry : rawMap.entrySet()) {
            String name = entry.getKey();
            String val = entry.getValue();
            String searchKey = query != null ? query.get("paramName") : null;
            if (searchKey != null && !searchKey.isEmpty() && !name.toLowerCase().contains(searchKey.toLowerCase())) {
                continue;
            }
            allRows.add(new String[]{name, val});
        }

        int start = (pageNum - 1) * pageSize;
        if (start >= allRows.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, allRows.size());
        return allRows.subList(start, end);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        Map<String, String> rawMap;
        if (getRedisUtil().isCluster()) {
            rawMap = getRedisUtil().executeOnFirstClusterNode(jedis -> jedis.configGet("*"));
        } else {
            rawMap = getRedisUtil().execute(jedis -> jedis.configGet("*"));
        }

        if (rawMap == null) return 0;

        String searchKey = query != null ? query.get("paramName") : null;
        if (searchKey == null || searchKey.isEmpty()) {
            return rawMap.size();
        }

        int count = 0;
        for (String name : rawMap.keySet()) {
            if (name.toLowerCase().contains(searchKey.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean contains(String id) {
        if (getRedisUtil().isCluster()) {
            return getRedisUtil().executeOnFirstClusterNode(jedis -> {
                Map<String, String> values = jedis.configGet(id);
                return values != null && !values.isEmpty();
            });
        }
        return getRedisUtil().execute(jedis -> {
            Map<String, String> values = jedis.configGet(id);
            return values != null && !values.isEmpty();
        });
    }

    @Override
    public Map<String, String> show(String id) {
        Map<String, String> rawMap;
        if (getRedisUtil().isCluster()) {
            rawMap = getRedisUtil().executeOnFirstClusterNode(jedis -> jedis.configGet(id));
        } else {
            rawMap = getRedisUtil().execute(jedis -> jedis.configGet(id));
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("paramName", id);
        if (rawMap != null && rawMap.containsKey(id)) {
            data.put("paramValue", rawMap.get(id));
        } else {
            data.put("paramValue", "");
        }
        data.put("readonly", READONLY_CONFIGS.contains(id) ? "是" : "否");
        return data;
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        if (!checkWriteAllowed(getCurrentRequest())) return;
        String name = data.get("paramName");
        String value = data.get("paramValue");

        if (name == null || name.trim().isEmpty()) {
            throw new Exception("参数名不能为空");
        }
        if (value == null) {
            throw new Exception("参数值不能为空");
        }

        if (READONLY_CONFIGS.contains(name)) {
            throw new Exception("禁止修改只读配置: " + name);
        }

        executeConfigSet(name, value);
        logConfigAudit("update", name, value);
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        if (!checkWriteAllowed(getCurrentRequest())) return;
        String name = data.get("paramName");
        String value = data.get("paramValue");

        if (name == null || name.trim().isEmpty()) {
            throw new Exception("参数名不能为空");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new Exception("参数值不能为空");
        }

        if (READONLY_CONFIGS.contains(name)) {
            throw new Exception("禁止修改只读配置: " + name);
        }

        executeConfigSet(name, value);
        logConfigAudit("add", name, value);
    }

    private void executeConfigSet(String name, String value) throws Exception {
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeWithCluster(jedisCluster -> {
                try {
                    
                    String result = jedisCluster.configSet(name, value);
                    if (!"OK".equals(result)) {
                        throw new RuntimeException("CONFIG SET 失败: " + result);
                    }
                } catch (redis.clients.jedis.exceptions.JedisDataException e) {
                    
                    throw new RuntimeException("集群模式不支持 CONFIG SET: " + e.getMessage());
                }
                return null;
            });
        } else {
            getRedisUtil().execute(jedis -> {
                String result = jedis.configSet(name, value);
                if (!"OK".equals(result)) {
                    throw new RuntimeException("CONFIG SET 失败: " + result);
                }
                return null;
            });
        }
    }

    private void logConfigAudit(String operationType, String name, String value) {
        AuditEntry audit = new AuditEntry();
        String operator = getCurrentRequest() != null
                ? getCurrentRequest().getParameter("operator") : null;
        audit.setOperator(operator != null && !operator.isEmpty() ? operator : "unknown");
        audit.setOperationType(operationType);
        audit.setTargetType("serverConfig");
        audit.setTargetId(name);
        audit.setInstanceName(RedisApp.getCurrentInstanceName());
        audit.setEnvType(getCurrentEnvType());
        audit.setResult("成功");
        audit.setDetail("set " + name + "=" + value);
        RedisApp.getAuditStore().log(audit);
    }
}
