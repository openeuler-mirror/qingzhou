package qingzhou.monitor.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.monitor.Metric;
import qingzhou.monitor.MetricProvider;
import qingzhou.registry.AppStub;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/prometheus", HttpHandler.HANDLE_METHODS + "=GET"})
public class PrometheusEndpoint implements HttpHandler {
    // 抓取最小采样间隔：远程采集是同步 RPC，实例多时线性变慢，短时间内的重复抓取直接复用缓存
    private static final long MIN_SCRAPE_INTERVAL_MS = 15_000;

    @Reference
    private Registry registry;
    @Reference
    private Logger logger;

    private final List<MetricProvider> metricProviders = new CopyOnWriteArrayList<>();
    // metricName 转换结果缓存：同一 field.code 每次 scrape 的转换结果不变
    private final Map<String, String> metricNameCache = new ConcurrentHashMap<>();
    private String cachedBody;
    private long lastScrapeAt;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void bindMetricProvider(MetricProvider provider) {
        metricProviders.add(provider);
    }

    protected void unbindMetricProvider(MetricProvider provider) {
        metricProviders.remove(provider);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        String body;
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (cachedBody == null || now - lastScrapeAt >= MIN_SCRAPE_INTERVAL_MS) {
                cachedBody = render();
                lastScrapeAt = now;
            }
            body = cachedBody;
        }

        httpResponse.contentType("text/plain; version=0.0.4; charset=utf-8")
                .send(body);
    }

    private String render() {
        Map<String, MetricGroup> groups = new LinkedHashMap<>();

        for (String appCode : registry.getAllLocalApps()) {
            AppStub stub = registry.getLocalApp(appCode);
            if (stub == null) continue;
            try {
                collect(groups, qingzhou.registry.Constants.LOCAL_INSTANCE_ID, appCode, stub);
            } catch (Throwable t) {
                logger.debug("skip failed local app metric collection: " + appCode, t);
            }
        }

        for (String instanceId : registry.getAllRemoteInstances()) {
            List<String> remoteApps = registry.getAllRemoteApps(instanceId);
            if (remoteApps == null) continue;
            for (String appCode : remoteApps) {
                AppStub stub = registry.getRemoteApp(instanceId, appCode);
                if (stub == null) continue;
                try {
                    collect(groups, instanceId, appCode, stub);
                } catch (Throwable t) {
                    logger.debug("skip failed remote app metric collection: " + appCode, t);
                }
            }
        }

        collectProviders(groups);

        StringBuilder result = new StringBuilder();
        for (MetricGroup group : groups.values()) {
            result.append("# HELP ").append(group.metricName).append(' ').append(group.help).append('\n');
            result.append("# TYPE ").append(group.metricName).append(" gauge\n");
            for (String line : group.lines) {
                result.append(line).append('\n');
            }
        }
        return result.toString();
    }

    private void collect(Map<String, MetricGroup> groups, String instanceId, String appCode, AppStub stub) {
        // 执行本地和远程应用的 monitor 动作，收集监控指标
        List<MonitorCollector.MonitorField> monitorFields = MonitorCollector.collectFields(instanceId, appCode, stub);

        for (MonitorCollector.MonitorField f : monitorFields) {
            addMetric(groups, instanceId, appCode, f.modelCode, f.field, f.value);
        }
    }

    private void addMetric(Map<String, MetricGroup> groups, String instanceId, String appCode,
                           String modelCode, ModelField field, String value) {
        Double number = toDouble(value);
        if (number == null) return;
        String metricName = metricNameCache.computeIfAbsent(field.code, code ->
                "qingzhou_" + code.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceFirst("_(count|total)$", ""));
        MetricGroup group = groups.get(metricName);
        if (group == null) {
            group = new MetricGroup();
            group.metricName = metricName;
            group.help = field.name != null && field.name.length > 0
                    ? field.name[0].replace("\\", "\\\\").replace("\n", "\\n")
                    : field.code;
            groups.put(metricName, group);
        }
        group.lines.add(metricName
                + "{instance=\"" + escapeLabel(instanceId)
                + "\",app=\"" + escapeLabel(appCode)
                + "\",model=\"" + escapeLabel(modelCode)
                + "\",field=\"" + escapeLabel(field.code) + "\"} "
                + number);
    }

    private void collectProviders(Map<String, MetricGroup> groups) {
        for (MetricProvider provider : metricProviders) {
            try {
                for (Metric metric : provider.collect()) {
                    addProviderMetric(groups, provider.getName(), metric);
                }
            } catch (Throwable t) {
                logger.debug("skip failed metric provider: " + provider.getName(), t);
            }
        }
    }

    private void addProviderMetric(Map<String, MetricGroup> groups, String providerName, Metric metric) {
        String metricName = sanitizeMetricName(metric.getName());
        if (metricName.isEmpty()) return;

        StringBuilder line = new StringBuilder(metricName)
                .append("{instance=\"").append(escapeLabel(Constants.LOCAL_INSTANCE_ID))
                .append("\",provider=\"").append(escapeLabel(providerName)).append('"');
        for (Map.Entry<String, String> label : metric.getLabels().entrySet()) {
            String labelKey = sanitizeLabelKey(label.getKey());
            if (labelKey == null) continue; // 非法标签名会让整段 exposition 解析失败，直接丢弃该标签
            line.append(',').append(labelKey).append("=\"").append(escapeLabel(label.getValue())).append('"');
        }
        line.append("} ").append(metric.getValue());

        MetricGroup group = groups.get(metricName);
        if (group == null) {
            group = new MetricGroup();
            group.metricName = metricName;
            group.help = metricName;
            groups.put(metricName, group);
        }
        group.lines.add(line.toString());
    }

    // 自动补 qingzhou_ 前缀，并清洗 Prometheus 非法字符
    private static String sanitizeMetricName(String name) {
        String sanitized = name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.isEmpty()) return sanitized;
        return sanitized.startsWith("qingzhou_") ? sanitized : "qingzhou_" + sanitized;
    }

    // 标签名须符合 [a-zA-Z_][a-zA-Z0-9_]*：provider 给出的 key 不可信，非法即丢弃该标签
    private static String sanitizeLabelKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
                    || (i > 0 && c >= '0' && c <= '9');
            if (!valid) return null;
        }
        return key;
    }

    // 标签值须转义反斜杠/引号/换行，否则可破坏 exposition 格式（label injection）
    private static String escapeLabel(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private Double toDouble(String value) {
        if ("true".equalsIgnoreCase(value)) return 1d;
        if ("false".equalsIgnoreCase(value)) return 0d;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class MetricGroup {
        String metricName;
        String help;
        final List<String> lines = new ArrayList<>();
    }
}
