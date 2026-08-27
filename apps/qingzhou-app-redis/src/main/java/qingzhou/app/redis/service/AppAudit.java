package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Page;
import qingzhou.api.type.Show;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.store.model.AuditEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "appAudit", icon = "DocumentChecked", menu = "redis-ops", order = 4,
        name = {"应用审计", "en:App Audit"},
        info = {"记录 Redis 实例相关的关键操作日志", "en:Audit logs for Redis instance operations"})
public class AppAudit extends RedisModelBase implements Page, Show {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"审计 ID", "en:Audit ID"},
            info = {"审计记录唯一标识", "en:Unique audit ID"})
    public String id;

    @ModelField(list = true, show = true,
            name = {"时间", "en:Time"},
            info = {"操作发生时间", "en:Operation time"})
    public String timestamp;

    @ModelField(list = true, show = true, search = true,
            name = {"操作人", "en:Operator"},
            info = {"操作人标识", "en:Operator identifier"})
    public String operator;

    @ModelField(list = true, show = true, search = true,
            options = {"add", "update", "delete", "execute", "switch", "confirm"},
            input_type = InputType.select,
            name = {"操作类型", "en:Operation Type"},
            info = {"操作类型", "en:Type of operation"})
    public String operationType;

    @ModelField(list = true, show = true,
            name = {"目标类型", "en:Target Type"},
            info = {"被操作模型或资源", "en:Target model or resource"})
    public String targetType;

    @ModelField(list = true, show = true,
            name = {"目标 ID", "en:Target ID"},
            info = {"被操作对象 ID", "en:Target object ID"})
    public String targetId;

    @ModelField(list = true, show = true,
            name = {"实例名", "en:Instance Name"},
            info = {"当前 Redis 实例名", "en:Current Redis instance name"})
    public String instanceName;

    @ModelField(list = true, show = true,
            name = {"环境", "en:Environment"},
            info = {"操作发生时的环境", "en:Environment when operation occurred"})
    public String envType;

    @ModelField(list = true, show = true,
            name = {"结果", "en:Result"},
            info = {"操作结果", "en:Operation result"})
    public String result;

    @ModelField(list = true, show = true,
            name = {"详情", "en:Detail"},
            info = {"操作详情", "en:Operation detail"})
    public String detail;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public java.util.List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        String keyword = query != null ? query.get("operator") : null;
        String type = query != null ? query.get("operationType") : null;
        if (type != null && type.isEmpty()) {
            type = null;
        }
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        return wrap(RedisApp.getAuditStore().query(keyword, type, pageNum, pageSize));
    }

    @Override
    public int totalSize(Map<String, String> query) {
        String keyword = query != null ? query.get("operator") : null;
        String type = query != null ? query.get("operationType") : null;
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        if (type != null && type.isEmpty()) {
            type = null;
        }
        return RedisApp.getAuditStore().total(keyword, type);
    }

    @Override
    public boolean contains(String id) {
        return RedisApp.getAuditStore().getById(id) != null;
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        AuditEntry entry = RedisApp.getAuditStore().getById(id);
        if (entry == null) {
            throw new Exception("审计记录不存在: " + id);
        }
        return toMap(entry);
    }

    private java.util.List<String[]> wrap(java.util.List<AuditEntry> entries) {
        java.util.List<String[]> rows = new ArrayList<>();
        for (AuditEntry entry : entries) {
            rows.add(toArray(entry));
        }
        return rows;
    }

    private String[] toArray(AuditEntry entry) {
        return new String[]{
                entry.getId(),
                entry.getTimestamp() > 0 ? Instant.ofEpochMilli(entry.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT) : "-",
                entry.getOperator(),
                entry.getOperationType(),
                entry.getTargetType(),
                entry.getTargetId(),
                entry.getInstanceName(),
                entry.getEnvType(),
                entry.getResult(),
                entry.getDetail()
        };
    }

    private Map<String, String> toMap(AuditEntry entry) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", entry.getId());
        data.put("timestamp", entry.getTimestamp() > 0 ? Instant.ofEpochMilli(entry.getTimestamp()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT) : "-");
        data.put("operator", entry.getOperator());
        data.put("operationType", entry.getOperationType());
        data.put("targetType", entry.getTargetType());
        data.put("targetId", entry.getTargetId());
        data.put("instanceName", entry.getInstanceName());
        data.put("envType", entry.getEnvType());
        data.put("result", entry.getResult());
        data.put("detail", entry.getDetail());
        return data;
    }
}
