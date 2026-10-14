package qingzhou.monitor.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.Constants;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/prometheus"})
public class PrometheusEndpoint implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private Logger logger;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        Map<String, MetricGroup> groups = new LinkedHashMap<>();

        for (String appCode : registry.getAllLocalApps()) {
            AppStub stub = registry.getLocalApp(appCode);
            if (stub == null) continue;
            try {
                collect(groups, Constants.LOCAL_INSTANCE_ID, appCode, stub);
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

        StringBuilder result = new StringBuilder();
        for (MetricGroup group : groups.values()) {
            result.append("# HELP ").append(group.metricName).append(' ').append(group.help).append('\n');
            result.append("# TYPE ").append(group.metricName).append(" gauge\n");
            for (String line : group.lines) {
                result.append(line).append('\n');
            }
        }

        httpResponse.contentType("text/plain; version=0.0.4; charset=utf-8")
                .send(result.toString())
                .finish();
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
        String metricName = "qingzhou_" + field.code.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceFirst("_(count|total)$", "");
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
