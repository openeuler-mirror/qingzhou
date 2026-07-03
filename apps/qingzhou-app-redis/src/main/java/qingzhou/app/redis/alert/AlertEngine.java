package qingzhou.app.redis.alert;

import qingzhou.app.redis.RedisApp;
import qingzhou.app.redis.store.AlertStore;
import qingzhou.app.redis.store.MetricsStore;
import qingzhou.app.redis.store.model.AlertRecord;
import qingzhou.app.redis.store.model.AlertRule;

import java.util.List;

public class AlertEngine implements Runnable {

    private static volatile AlertEngine instance;

    private final AlertStore alertStore;
    private final MetricsStore metricsStore;

    public AlertEngine(AlertStore alertStore, MetricsStore metricsStore) {
        this.alertStore = alertStore;
        this.metricsStore = metricsStore;
    }

    public static AlertEngine initialize(AlertStore alertStore, MetricsStore metricsStore) {
        instance = new AlertEngine(alertStore, metricsStore);
        return instance;
    }

    public static AlertEngine getInstance() {
        return instance;
    }

    @Override
    public void run() {

        if (RedisApp.getRedisUtil() == null) {
            return;
        }
        List<AlertRule> rules = alertStore.listRules();
        for (AlertRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            try {
                checkRule(rule);
            } catch (Exception ignored) {
            }
        }
    }

    private void checkRule(AlertRule rule) {
        Double latest = metricsStore.getLatest(rule.getMetric());
        if (latest == null) {
            return;
        }
        boolean triggered = compare(latest, rule.getThreshold(), rule.getComparator());
        if (!triggered) {
            return;
        }

        for (AlertRecord record : alertStore.getRecords()) {
            if (rule.getId().equals(record.getRuleId()) && "未确认".equals(record.getStatus())) {
                return;
            }
        }
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getName());
        record.setValue(latest);
        record.setThreshold(rule.getThreshold());
        record.setLevel(rule.getLevel());
        record.setStatus("未确认");
        record.setTriggeredAt(System.currentTimeMillis());
        alertStore.addRecord(record);
    }

    private boolean compare(double value, double threshold, String comparator) {
        if (comparator == null) {
            return false;
        }
        switch (comparator.trim()) {
            case ">":
                return value > threshold;
            case ">=":
                return value >= threshold;
            case "<":
                return value < threshold;
            case "<=":
                return value <= threshold;
            case "=":
            case "==":
                return Double.compare(value, threshold) == 0;
            case "!=":
                return Double.compare(value, threshold) != 0;
            default:
                return false;
        }
    }
}
