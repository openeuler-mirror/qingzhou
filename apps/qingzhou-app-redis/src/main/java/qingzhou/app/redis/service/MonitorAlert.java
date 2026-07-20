package qingzhou.app.redis.service;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Show;
import qingzhou.api.type.Update;
import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.RedisModelBase;
import qingzhou.app.redis.alert.AlertEngine;
import qingzhou.app.redis.store.model.AlertRecord;
import qingzhou.app.redis.store.model.AlertRule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Model(code = "monitorAlert", icon = "Bell", menu = "redis-monitor", order = 2,
        name = {"监控告警", "en:Monitor Alert"},
        info = {"告警规则配置与告警历史", "en:Alert rules and alert history"})
public class MonitorAlert extends RedisModelBase implements qingzhou.api.type.List, Show, Add, Update, Delete {


    @ModelField(id = true, list = true, show = true, add = false, update = false, readonly = true,
            name = {"规则 ID", "en:Rule ID"},
            info = {"告警规则唯一标识", "en:Unique alert rule ID"})
    public String id;

    @ModelField(list = true, show = true, add = true, update = true, required = true,
            name = {"规则名称", "en:Rule Name"},
            info = {"告警规则名称", "en:Alert rule name"})
    public String name;

    @ModelField(list = true, show = true, add = true, update = true, required = true,
            options = {"redis.used_memory", "redis.connected_clients", "redis.instantaneous_ops_per_sec",
                    "redis.hit_rate", "redis.mem_fragmentation_ratio", "redis.latency_ms",
                    "redis.rejected_connections", "redis.total_keys", "machine.process_cpu_usage",
                    "machine.system_cpu_usage", "machine.memory_usage_rate"},
            input_type = InputType.select,
            name = {"监控指标", "en:Metric"},
            info = {"要监控的指标", "en:Metric to monitor"})
    public String metric;

    @ModelField(show = true, add = true, update = true, required = true,
            options = {">", ">=", "<", "<=", "=", "!="},
            input_type = InputType.select,
            name = {"比较方式", "en:Comparator"},
            info = {"指标值与阈值的比较方式", "en:Comparison operator"})
    public String comparator;

    @ModelField(show = true, add = true, update = true, required = true, numeric = true,
            input_type = InputType.number,
            name = {"阈值", "en:Threshold"},
            info = {"告警阈值", "en:Alert threshold"})
    public String threshold;

    @ModelField(list = true, show = true, add = true, update = true, required = true,
            options = {"严重", "警告"},
            input_type = InputType.select,
            name = {"告警级别", "en:Level"},
            info = {"告警级别", "en:Alert level"})
    public String level;

    @ModelField(list = true, show = true, add = true, update = true,
            input_type = InputType.checkbox,
            options = {"true", "false"},
            name = {"启用状态", "en:Enabled"},
            info = {"是否启用该规则", "en:Whether the rule is enabled"})
    public String enabled;


    @ModelField(show = true,
            name = {"触发值", "en:Triggered Value"},
            info = {"触发告警时的指标值", "en:Metric value when triggered"})
    public String triggeredValue;

    @ModelField(show = true,
            name = {"阈值", "en:Threshold"},
            info = {"告警阈值", "en:Alert threshold"})
    public String historyThreshold;

    @ModelField(show = true,
            color = {"严重:#F56C6C", "警告:#E6A23C"},
            name = {"级别", "en:Level"},
            info = {"告警级别", "en:Alert level"})
    public String historyLevel;

    @ModelField(show = true,
            color = {"未确认:#F56C6C", "已确认:#67C23A"},
            name = {"状态", "en:Status"},
            info = {"告警状态", "en:Alert status"})
    public String status;

    @ModelField(show = true,
            name = {"触发时间", "en:Triggered At"},
            info = {"告警触发时间", "en:Alert trigger time"})
    public String triggeredAt;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public java.util.List<String[]> list(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        java.util.List<AlertRule> rules = RedisApp.getAlertStore().listRules();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, rules.size());
        java.util.List<AlertRule> page = start < rules.size() ? rules.subList(start, end) : new ArrayList<>();
        java.util.List<String[]> rows = new ArrayList<>();
        for (AlertRule rule : page) {
            rows.add(new String[]{
                    rule.getId(),
                    rule.getName(),
                    rule.getMetric(),
                    rule.getLevel(),
                    String.valueOf(rule.isEnabled())
            });
        }
        return rows;
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return RedisApp.getAlertStore().listRules().size();
    }

    @Override
    public boolean contains(String id) {
        if (id == null) return false;
        return RedisApp.getAlertStore().getRule(id) != null;
    }

    @Override
    public Map<String, String> show(String id) throws Exception {
        AlertRule rule = RedisApp.getAlertStore().getRule(id);
        if (rule != null) {
            return ruleToMap(rule);
        }
        AlertRecord record = RedisApp.getAlertStore().getRecord(id);
        if (record != null) {
            return recordToMap(record);
        }
        throw new Exception("告警记录不存在: " + id);
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        checkWriteAllowed();
        validateRequiredFields(data);
        AlertRule rule = new AlertRule();
        rule.setName(data.get("name"));
        rule.setMetric(data.get("metric"));
        rule.setComparator(data.get("comparator"));
        rule.setThreshold(Double.parseDouble(data.get("threshold")));
        rule.setLevel(data.get("level"));
        rule.setEnabled(!"false".equalsIgnoreCase(data.get("enabled")));
        RedisApp.getAlertStore().addRule(rule);

        AlertEngine engine = AlertEngine.getInstance();
        if (engine != null) {
            engine.run();
        }
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        checkWriteAllowed();
        validateRequiredFields(data);
        AlertRule existing = RedisApp.getAlertStore().getRule(id);
        if (existing == null) {
            throw new Exception("告警规则不存在: " + id);
        }
        existing.setName(data.get("name"));
        existing.setMetric(data.get("metric"));
        existing.setComparator(data.get("comparator"));
        existing.setThreshold(Double.parseDouble(data.get("threshold")));
        existing.setLevel(data.get("level"));
        existing.setEnabled(!"false".equalsIgnoreCase(data.get("enabled")));
        RedisApp.getAlertStore().updateRule(existing);
    }

    @Override
    public void delete(String id) throws Exception {
        checkDangerousAllowed();
        RedisApp.getAlertStore().deleteRule(id);
    }

    @ModelAction(name = {"确认告警", "en:Confirm Alert"},
            info = {"确认该告警已处理", "en:Confirm this alert has been handled"})
    public void confirm(Request request) throws Exception {
        String id = request.getId();
        String operator = request.getParameter("operator");
        if (operator == null || operator.isEmpty()) {
            operator = "unknown";
        }
        RedisApp.getAlertStore().confirmRecord(id, operator);
    }

    private void validateRequiredFields(Map<String, String> data) throws Exception {
        String[] requiredFields = {"name", "metric", "comparator", "threshold", "level"};
        for (String field : requiredFields) {
            String value = data.get(field);
            if (value == null || value.trim().isEmpty()) {
                throw new Exception("必填字段不能为空: " + field);
            }
        }
    }

    private Map<String, String> ruleToMap(AlertRule rule) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", rule.getId());
        data.put("name", rule.getName());
        data.put("metric", rule.getMetric());
        data.put("comparator", rule.getComparator());
        data.put("threshold", String.valueOf(rule.getThreshold()));
        data.put("level", rule.getLevel());
        data.put("enabled", String.valueOf(rule.isEnabled()));
        return data;
    }

    private Map<String, String> recordToMap(AlertRecord record) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", record.getId());
        data.put("name", record.getRuleName());
        data.put("triggeredValue", String.valueOf(record.getValue()));
        data.put("historyThreshold", String.valueOf(record.getThreshold()));
        data.put("historyLevel", record.getLevel());
        data.put("status", record.getStatus());
        data.put("triggeredAt", record.getTriggeredAt() > 0 ? Instant.ofEpochMilli(record.getTriggeredAt()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT) : "-");
        return data;
    }
}
