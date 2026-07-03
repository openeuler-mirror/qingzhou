package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.diagnosis.DiagnosticEngine;
import qingzhou.app.redis.util.RedisUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "appOperation", icon = "Tools", menu = "redis-ops", order = 2,
        name = {"应用运维", "en:App Operation"},
        info = {"Redis 实例主动运维操作", "en:Active Redis instance operations"})
public class AppOperation extends RedisModelBase implements List {

    @ModelField(id = true, list = true, readonly = true,
            name = {"运维项 ID", "en:Operation ID"},
            info = {"运维项唯一标识", "en:Unique operation ID"})
    public String id;

    @ModelField(list = true,
            name = {"运维项名称", "en:Operation Name"},
            info = {"运维项名称", "en:Operation name"})
    public String operationName;

    @ModelField(list = true,
            name = {"当前状态", "en:Current Status"},
            info = {"当前状态", "en:Current status"})
    public String currentStatus;

    @ModelField(list = true,
            name = {"最近检查时间", "en:Last Check Time"},
            info = {"最近一次检查时间", "en:Last check time"})
    public String lastCheckTime;

    private static final String[] OP_IDS = {"ping", "reconnect"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<String[]> rows = new ArrayList<>();
        RedisUtil util = RedisApp.getRedisUtil();
        String checkTime = LocalDateTime.now().format(DATE_FORMAT);
        for (String opId : OP_IDS) {
            String status = getStatus(util, opId);
            rows.add(new String[]{opId, getName(opId), status, checkTime});
        }
        int start = (pageNum - 1) * pageSize;
        if (start >= rows.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, rows.size());
        return rows.subList(start, end);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return OP_IDS.length;
    }

    @Override
    public boolean contains(String id) {
        return Arrays.asList(OP_IDS).contains(id);
    }

    @ModelAction(name = {"全节点 PING", "en:Ping All Nodes"},
            info = {"对所有节点执行 PING 探测", "en:Ping all nodes"})
    public void ping(String id) throws Exception {
        RedisUtil util = getRedisUtil();
        long latency = util.latencyPing();
        if (latency < 0) {
            throw new Exception("PING 探测失败");
        }
    }

    @ModelAction(name = {"重新连接", "en:Reconnect"},
            confirm = {"确认重新连接当前实例？", "en:Confirm reconnect to current instance?"},
            info = {"关闭并重新初始化当前实例连接", "en:Close and reinitialize current instance connection"})
    public void reconnect(String id) throws Exception {
        String current = RedisApp.getCurrentInstanceName();
        if (current == null) {
            throw new Exception("未激活任何实例");
        }
        RedisApp.activateInstance(current);
    }

    private String getStatus(RedisUtil util, String opId) {
        if (util == null || !util.isConnected()) {
            return "未连接";
        }
        switch (opId) {
            case "ping":
                return util.latencyPing() >= 0 ? "正常" : "异常";
            case "reconnect":
                return "可用";
            default:
                return "未知";
        }
    }

    private String getName(String opId) {
        switch (opId) {
            case "ping":
                return "全节点 PING";
            case "reconnect":
                return "重新连接";
            default:
                return opId;
        }
    }
}
