package qingzhou.app.redis.service;

import java.util.*;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.store.model.AuditEntry;

@Model(code = "redisInstance", icon = "Server", menu = "", order = 0,
        name = {"实例管理", "en:Instance Manager"},
        info = {"只读展示 Redis 连接实例，支持通过切换按钮或下拉框选择当前使用的实例。",
                "en:Display Redis connection instances in read-only mode, switch via button or dropdown."})
public class RedisInstance extends RedisModelBase implements qingzhou.api.type.List, Show, SwitchSpace {

    @ModelField(id = true, show = true, list = true, readonly = true,
            name = {"ID", "en:ID"},
            info = {"实例唯一标识", "en:Unique instance ID"})
    public String id;

    @ModelField(list = true, show = true, search = true, required = true,
            name = {"实例名称", "en:Instance Name"},
            info = {"Redis 实例自定义名称", "en:Redis instance display name"})
    public String instanceName;

    @ModelField(list = true, show = true, required = true,
            options = {"standalone", "sentinel", "cluster"},
            input_type = InputType.select,
            name = {"连接模式", "en:Mode"},
            info = {"连接模式：standalone 单机、sentinel 哨兵、cluster 集群", "en:Connection mode"})
    public String mode = "standalone";

    @ModelField(list = true, show = true, required = true,
            display = "mode==standalone",
            name = {"主机地址", "en:Host"},
            info = {"单机模式的 Redis 主机地址", "en:Redis host for standalone mode"})
    public String host;

    @ModelField(list = true, show = true, numeric = true, required = true,
            input_type = InputType.number,
            min = 1, max = 65535,
            display = "mode==standalone",
            name = {"端口", "en:Port"},
            info = {"单机模式的 Redis 端口（1-65535）", "en:Redis port for standalone mode (1-65535)"})
    public String port = "6379";

    @ModelField(show = true, input_type = InputType.password,
            name = {"密码", "en:Password"},
            info = {"Redis 连接密码", "en:Redis connection password"})
    public String password;

    @ModelField(show = true, numeric = true,
            input_type = InputType.number,
            min = 0, max = 15,
            display = "mode==standalone",
            name = {"数据库", "en:Database"},
            info = {"单机模式使用的数据库编号（0-15）", "en:Database index for standalone mode (0-15)"})
    public String database;

    @ModelField(show = true, input_type = InputType.textarea,
            display = "mode==sentinel",
            name = {"哨兵节点", "en:Sentinel Nodes"},
            info = {"哨兵模式节点列表，每行一个 host:port", "en:Sentinel nodes, one host:port per line"})
    public String sentinelNodes;

    @ModelField(show = true,
            display = "mode==sentinel",
            name = {"哨兵 Master", "en:Sentinel Master"},
            info = {"哨兵模式的 Master 名称", "en:Master name for sentinel mode"})
    public String sentinelMaster;

    @ModelField(show = true, input_type = InputType.textarea,
            display = "mode==cluster",
            name = {"集群节点", "en:Cluster Nodes"},
            info = {"集群模式节点列表，每行一个 host:port", "en:Cluster nodes, one host:port per line"})
    public String clusterNodes;

    @ModelField(list = true, show = true, required = true,
            options = {"production", "development", "test"},
            input_type = InputType.select,
            color = {"production:#F56C6C", "test:#E6A23C", "development:#67C23A"},
            name = {"环境标识", "en:Environment"},
            info = {"production 生产环境（限制破坏性操作）、development 开发环境、test 测试环境",
                    "en:production (restrict destructive ops), development, test"})
    public String envType;

    @ModelField(list = true, show = true, readonly = true,
            color = {"是:#67C23A", "否:#909399"},
            name = {"当前活跃", "en:Active"},
            info = {"是否为当前活跃连接", "en:Whether this is the active connection"})
    public String active;

    @ModelField(list = true, show = true, readonly = true,
            name = {"连接状态", "en:Status"},
            info = {"连接状态", "en:Connection status"})
    public String status;

    @ModelField(list = true, show = true, readonly = true,
            name = {"最大内存", "en:Max Memory"},
            info = {"Redis 最大内存配置（字节）", "en:Redis maxmemory configuration in bytes"})
    public String maxmemory;

    @ModelField(list = true, show = true, readonly = true,
            name = {"最大客户端数", "en:Max Clients"},
            info = {"Redis 最大客户端连接数", "en:Redis maxclients configuration"})
    public String maxclients;

    @ModelAction(name = {"激活", "en:Activate"},
            confirm = {"确认激活该 Redis 实例？将切换当前连接。", "en:Confirm activate this Redis instance? Current connection will switch."},
            info = {"切换到该 Redis 实例连接", "en:Switch to this Redis instance connection"})
    public void switchSpace(String id) throws Exception {
        String previousName = RedisApp.getCurrentInstanceName();
        try {
            if (id != null && !id.isEmpty()) {
                RedisApp.activateInstance(id);
            } else {
                RedisApp.deactivateInstance();
            }

            AuditEntry audit = new AuditEntry();
            String operator = getCurrentRequest() != null
                    ? getCurrentRequest().getParameter("operator") : null;
            audit.setOperator(operator != null && !operator.isEmpty() ? operator : "unknown");
            audit.setOperationType("switch");
            audit.setTargetType("redisInstance");
            audit.setTargetId(id);
            audit.setInstanceName(RedisApp.getCurrentInstanceName());
            audit.setEnvType(getCurrentEnvType());
            audit.setResult("成功");
            audit.setDetail("switch to instance: " + RedisApp.getCurrentInstanceName());
            RedisApp.getAuditStore().log(audit);
        } catch (Exception e) {
            
            if (previousName != null && RedisApp.getInstances().containsKey(previousName)) {
                try {
                    RedisApp.activateInstance(previousName);
                } catch (Exception restoreEx) {
                    
                }
            }
            throw e;
        }
    }

    @Override
    public String currentSpace() {
        String currentName = RedisApp.getCurrentInstanceName();
        if (currentName == null || currentName.isEmpty()) {
            return null;
        }
        Map<String, Map<String, String>> instances = RedisApp.getInstances();
        Map<String, String> config = instances.get(currentName);
        if (config == null) return null;
        String id = config.get("id");
        return (id != null && !id.isEmpty()) ? id : null;
    }

    

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize,
                                         Map<String, String> query, String[] listFields) throws Exception {
        Map<String, Map<String, String>> instances = RedisApp.getInstances();
        java.util.List<String[]> rows = new ArrayList<>();
        String currentName = RedisApp.getCurrentInstanceName();

        for (Map.Entry<String, Map<String, String>> entry : instances.entrySet()) {
            String name = entry.getKey();
            Map<String, String> config = entry.getValue();

            String searchKey = query != null ? query.get("instanceName") : null;
            if (searchKey != null && !searchKey.isEmpty() && !name.toLowerCase().contains(searchKey.toLowerCase())) {
                continue;
            }

            String isActive = name.equals(currentName) ? "是" : "否";
            boolean connected = isActive.equals("是") && RedisApp.getRedisUtil() != null && RedisApp.getRedisUtil().isConnected();
            String statusStr = connected ? "已连接" : "未连接";
            String maxmemory = connected ? getConfigValue("maxmemory") : "-";
            String maxclients = connected ? getConfigValue("maxclients") : "-";

            rows.add(new String[]{
                    config.getOrDefault("id", ""),
                    name,
                    config.getOrDefault("mode", "standalone"),
                    config.getOrDefault("host", "127.0.0.1"),
                    config.getOrDefault("port", "6379"),
                    config.getOrDefault("envType", "development"),
                    isActive,
                    statusStr,
                    maxmemory,
                    maxclients
            });
        }

        int start = (pageNum - 1) * pageSize;
        if (start >= rows.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, rows.size());
        return rows.subList(start, end);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String searchKey = query != null ? query.get("instanceName") : null;
        if (searchKey == null || searchKey.isEmpty()) {
            return RedisApp.getInstances().size();
        }
        int count = 0;
        for (String name : RedisApp.getInstances().keySet()) {
            if (name.toLowerCase().contains(searchKey.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean contains(String id) {
        
        for (Map<String, String> config : RedisApp.getInstances().values()) {
            if (id.equals(config.get("id"))) return true;
        }
        
        return RedisApp.getInstances().containsKey(id);
    }

    

    @Override
    public Map<String, String> show(String id) throws Exception {
        
        String name = null;
        Map<String, String> config = null;
        for (Map.Entry<String, Map<String, String>> entry : RedisApp.getInstances().entrySet()) {
            if (id.equals(entry.getValue().get("id")) || id.equals(entry.getKey())) {
                name = entry.getKey();
                config = entry.getValue();
                break;
            }
        }
        if (config == null) throw new Exception("实例不存在: " + id);

        Map<String, String> data = new LinkedHashMap<>(config);
        data.put("id", config.getOrDefault("id", ""));
        data.put("instanceName", name);
        
        String storedPassword = config.getOrDefault("password", "");
        if (storedPassword.startsWith(RedisApp.getEncryptedPrefix())) {
            data.put("password", ""); 
        }
        data.put("active", name.equals(RedisApp.getCurrentInstanceName()) ? "是" : "否");
        boolean connected = name.equals(RedisApp.getCurrentInstanceName()) && RedisApp.getRedisUtil() != null
                && RedisApp.getRedisUtil().isConnected();
        data.put("status", connected ? "已连接" : "未连接");
        data.put("maxmemory", connected ? getConfigValue("maxmemory") : "-");
        data.put("maxclients", connected ? getConfigValue("maxclients") : "-");
        return data;
    }

    private String getConfigValue(String key) {
        try {
            if (RedisApp.getRedisUtil().isCluster()) {
                return RedisApp.getRedisUtil().executeOnFirstClusterNode(jedis -> {
                    Map<String, String> values = jedis.configGet(key);
                    return values != null && !values.isEmpty() ? values.values().iterator().next() : "-";
                });
            } else {
                return RedisApp.getRedisUtil().execute(jedis -> {
                    Map<String, String> values = jedis.configGet(key);
                    return values != null && !values.isEmpty() ? values.values().iterator().next() : "-";
                });
            }
        } catch (Exception e) {
            return "-";
        }
    }
}
