package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Page;
import qingzhou.app.redis.RedisModelBase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Slowlog;

import java.text.SimpleDateFormat;
import java.util.*;

@Model(code = "slowLog", icon = "Clock", menu = "redis-monitor", order = 3,
        name = {"慢查询日志", "en:Slow Log"},
        info = {"查看 Redis 慢查询日志，用于排查性能问题。", "en:View Redis slow query logs for performance diagnosis."})
public class SlowLog extends RedisModelBase implements Page, Delete {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"日志 ID", "en:Log ID"},
            info = {"慢查询日志的唯一标识", "en:Unique identifier of the slow query log"})
    public String logId;

    @ModelField(list = true, show = true, readonly = true,
            name = {"执行时间", "en:Timestamp"},
            info = {"命令执行的时间", "en:Time when the command was executed"})
    public String timestamp;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"耗时(微秒)", "en:Duration(us)"},
            info = {"命令执行的耗时（微秒）", "en:Execution duration in microseconds"})
    public String duration;

    @ModelField(list = true, show = true, readonly = true,
            name = {"命令", "en:Command"},
            info = {"执行的命令和参数", "en:Executed command and arguments"})
    public String command;

    @ModelField(list = true, show = true, readonly = true,
            name = {"客户端", "en:Client"},
            info = {"发起命令的客户端 IP:端口", "en:IP:port of the client that issued the command"})
    public String clientIp;

    

    @Override
    public java.util.List<String[]> page(int pageNum, int pageSize,
                                         Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<Object[]> allLogs = Collections.synchronizedList(new ArrayList<>());

        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectSlowLogs(jedis, allLogs));
        } else {
            getRedisUtil().execute(jedis -> {
                collectSlowLogs(jedis, allLogs);
                return null;
            });
        }

        allLogs.sort((a, b) -> Long.compare((Long) b[1], (Long) a[1]));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int start = (pageNum - 1) * pageSize;
        if (start >= allLogs.size()) return new ArrayList<>();

        int end = Math.min(start + pageSize, allLogs.size());
        java.util.List<String[]> rows = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Object[] entry = allLogs.get(i);
            String[] row = new String[5];
            row[0] = String.valueOf(entry[0]);
            row[1] = sdf.format(new Date((Long) entry[1] * 1000));
            row[2] = String.valueOf(entry[2]);
            row[3] = (String) entry[3];
            row[4] = (String) entry[4];
            rows.add(row);
        }
        return rows;
    }

    private void collectSlowLogs(Jedis jedis, java.util.List<Object[]> collector) {
        List<Slowlog> logs = jedis.slowlogGet(128);
        if (logs == null) return;
        for (Slowlog log : logs) {
            long id = log.getId();
            long ts = log.getTimeStamp();
            long duration = log.getExecutionTime();
            List<String> cmdParts = log.getArgs();
            StringBuilder cmd = new StringBuilder();
            for (int i = 0; i < cmdParts.size(); i++) {
                if (cmd.length() > 0) cmd.append(" ");
                cmd.append(cmdParts.get(i));
            }
            String clientAddr = log.getClientIpPort() != null ? log.getClientIpPort().toString() : "-";
            collector.add(new Object[]{id, ts, duration, cmd.toString(), clientAddr});
        }
    }

    @Override
    public int totalSize(Map<String, String> query) {
        if (getRedisUtil().isCluster()) {
            int[] total = {0};
            getRedisUtil().executeOnAllClusterNodes(jedis -> {
                total[0] += (int) jedis.slowlogLen();
            });
            return total[0];
        }
        return getRedisUtil().execute(jedis -> (int) jedis.slowlogLen());
    }

    @Override
    public boolean contains(String id) {
        if (id == null || id.isEmpty()) return false;
        long targetId;
        try {
            targetId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return false;
        }

        java.util.List<Object[]> allLogs = Collections.synchronizedList(new ArrayList<>());
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> collectSlowLogs(jedis, allLogs));
        } else {
            getRedisUtil().execute(jedis -> {
                collectSlowLogs(jedis, allLogs);
                return null;
            });
        }
        for (Object[] entry : allLogs) {
            if (targetId == (Long) entry[0]) return true;
        }
        return false;
    }

    

    @Override
    public void delete(String id) throws Exception {
        checkDangerousAllowed();
        if (getRedisUtil().isCluster()) {
            getRedisUtil().executeOnAllClusterNodes(jedis -> jedis.slowlogReset());
        } else {
            getRedisUtil().execute(jedis -> {
                jedis.slowlogReset();
                return null;
            });
        }
    }
}