package qingzhou.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.FieldType;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/monitor", HttpHandler.HANDLE_NO_AUTH + "=true"})
public class Monitor implements HttpHandler {
    @Reference
    private Registry registry;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Map<String, MetricGroup> groups = new LinkedHashMap<>();
        InstanceInfo thisInstanceInfo = Heartbeat.thisInstanceInfo;
        String instanceId = thisInstanceInfo != null ? thisInstanceInfo.getId() : Constants.LOCAL_INSTANCE_ID;

        for (String appCode : registry.getAllLocalApps()) {
            collect(groups, instanceId, appCode, registry.getLocalApp(appCode));
        }

        StringBuilder sb = new StringBuilder();
        for (MetricGroup group : groups.values()) {
            sb.append("# HELP ").append(group.metricName).append(' ').append(group.help).append('\n');
            sb.append("# TYPE ").append(group.metricName).append(" gauge\n");
            for (String line : group.lines) {
                sb.append(line).append('\n');
            }
        }

        httpResponse.contentType("text/plain; version=0.0.4; charset=utf-8").sendFinish(sb.toString());
    }

    private void collect(Map<String, MetricGroup> groups, String instanceId, String appCode, AppStubLocal stub) {
        for (Model model : stub.getAppMeta().getApp().models) {
            if (!hasMonitorAction(model)) continue;

            RequestImpl request = new RequestImpl();
            request.setInstance(Constants.LOCAL_INSTANCE_ID);
            request.setApp(appCode);
            request.setModel(model.code);
            request.setAction(qingzhou.api.action.Monitor.ACTION_CODE_MONITOR);
            try {
                stub.invokeApp(request);
            } catch (Throwable ignored) {
                continue;
            }

            Object data = request.getResponse().getData();
            if (!(data instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, String> monitorData = (Map<String, String>) data;

            for (ModelField field : model.fields) {
                if (field.field_type != FieldType.monitor) continue;
                String value = monitorData.get(field.code);
                if (value == null) continue;
                addMetric(groups, instanceId, appCode, model.code, field, value);
            }
        }
    }

    private boolean hasMonitorAction(Model model) {
        for (ModelAction action : model.actions) {
            if (qingzhou.api.action.Monitor.ACTION_CODE_MONITOR.equals(action.code)) return true;
        }
        return false;
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
                + "{instance=\"" + instanceId
                + "\",app=\"" + appCode
                + "\",model=\"" + modelCode
                + "\",field=\"" + field.code + "\"} "
                + number);
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
