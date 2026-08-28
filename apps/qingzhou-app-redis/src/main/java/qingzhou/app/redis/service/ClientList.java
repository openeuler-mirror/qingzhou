package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.action.Page;
import qingzhou.app.redis.RedisModelBase;
import redis.clients.jedis.Jedis;

import java.util.*;

@Model(code = "clientList", icon = "UserFilled", menu = "redis-monitor", order = 4,
        name = {"客户端列表", "en:Client List"},
        info = {"查看所有已连接的 Redis 客户端信息。", "en:View all connected Redis client information."})
public class ClientList extends RedisModelBase implements Page {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"客户端 ID", "en:Client ID"},
            info = {"客户端连接的唯一标识", "en:Unique identifier of the client connection"})
    public String clientId;

    @ModelField(list = true, show = true, readonly = true, search = true,
            name = {"客户端地址", "en:Client Address"},
            info = {"客户端连接的 IP 地址和端口", "en:IP address and port of the client connection"})
    public String clientAddr;

    @ModelField(list = true, show = true, readonly = true,
            name = {"客户端名称", "en:Client Name"},
            info = {"通过 CLIENT SETNAME 设置的客户端名称", "en:Client name set via CLIENT SETNAME"})
    public String clientName;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"连接时长(秒)", "en:Age(sec)"},
            info = {"客户端连接的总持续时间", "en:Total duration of the connection in seconds"})
    public String age;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"空闲时间(秒)", "en:Idle(sec)"},
            info = {"客户端连接的空闲时间", "en:Idle time of the connection in seconds"})
    public String idle;

    @ModelField(list = true, show = true, readonly = true,
            name = {"数据库", "en:Database"},
            info = {"当前使用的数据库编号", "en:Current database index"})
    public String db;

    @ModelField(list = true, show = true, readonly = true,
            name = {"最后命令", "en:Last Command"},
            info = {"客户端最后执行的命令", "en:Last command executed by the client"})
    public String cmd;

    @Override
    public java.util.List<String[]> page(int pageNum, int pageSize,
                                         Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> allClients = Collections.synchronizedList(new ArrayList<>());

        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectClientList(jedis, allClients));
        } else {
            getRedisUtil().execute(jedis -> {
                collectClientList(jedis, allClients);
                return null;
            });
        }

        String searchAddr = query != null ? query.get("clientAddr") : null;
        if (searchAddr != null && !searchAddr.isEmpty()) {
            allClients.removeIf(row -> !row[1].toLowerCase().contains(searchAddr.toLowerCase()));
        }

        int start = (pageNum - 1) * pageSize;
        if (start >= allClients.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, allClients.size());
        return allClients.subList(start, end);
    }

    private void collectClientList(Jedis jedis, java.util.List<String[]> collector) {
        String clientListStr = jedis.clientList();
        if (clientListStr == null || clientListStr.isEmpty()) return;

        for (String line : clientListStr.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Map<String, String> props = new LinkedHashMap<>();
            for (String part : line.split(" ")) {
                int eqIdx = part.indexOf('=');
                if (eqIdx > 0) {
                    props.put(part.substring(0, eqIdx), part.substring(eqIdx + 1));
                }
            }

            String[] row = new String[7];
            row[0] = props.getOrDefault("id", "-");
            row[1] = props.getOrDefault("addr", "-");
            row[2] = props.getOrDefault("name", "");
            row[3] = props.getOrDefault("age", "0");
            row[4] = props.getOrDefault("idle", "0");
            row[5] = props.getOrDefault("db", "0");
            row[6] = props.getOrDefault("cmd", "");
            collector.add(row);
        }
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String searchAddr = query != null ? query.get("clientAddr") : null;
        if (searchAddr == null || searchAddr.isEmpty()) {
            if (getRedisUtil().isCluster()) {
                int[] total = {0};
                getRedisUtil().executeOnAllClusterNodes(jedis -> {
                    String list = jedis.clientList();
                    total[0] += list != null ? list.split("\n").length : 0;
                });
                return total[0];
            }
            return getRedisUtil().execute(jedis -> {
                String list = jedis.clientList();
                return list != null ? list.split("\n").length : 0;
            });
        }

        java.util.List<String[]> allClients = Collections.synchronizedList(new ArrayList<>());
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectClientList(jedis, allClients));
        } else {
            getRedisUtil().execute(jedis -> {
                collectClientList(jedis, allClients);
                return null;
            });
        }
        allClients.removeIf(row -> !row[1].toLowerCase().contains(searchAddr.toLowerCase()));
        return allClients.size();
    }

    @Override
    public boolean contains(String id) {
        if (id == null || id.isEmpty()) return false;

        java.util.List<String[]> allClients = Collections.synchronizedList(new ArrayList<>());
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectClientList(jedis, allClients));
        } else {
            getRedisUtil().execute(jedis -> {
                collectClientList(jedis, allClients);
                return null;
            });
        }
        for (String[] row : allClients) {
            if (id.equals(row[0])) return true;
        }
        return false;
    }
}