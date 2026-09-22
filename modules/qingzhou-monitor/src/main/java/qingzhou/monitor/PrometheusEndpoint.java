package qingzhou.monitor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.InstanceInfo;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Response;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.registry.AppStub;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/prometheus"})
public class PrometheusEndpoint implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private HttpClient httpClient;
    @Reference
    private CustomAuthenticator customAuthenticator;

    @Override
    public Authenticator customAuthenticator() {
        return request -> customAuthenticator.authenticate(request);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        Map<String, MetricGroup> groups = new LinkedHashMap<>();

        for (String appCode : registry.getAllLocalApps()) {
            collect(groups, Constants.LOCAL_INSTANCE_ID, appCode, registry.getLocalApp(appCode));
        }

        for (String instanceId : registry.getAllRemoteInstances()) {
            InstanceInfo info = registry.getRemoteInstance(instanceId);
            if (info == null) continue;
            String protocol = info.isSslEnabled() ? "https" : "http";
            String url = protocol + "://" + info.getHost() + ":" + info.getPort() + "/agent/monitor";
            try {
                Response response = httpClient.send(httpClient.newRequest(url).connectTimeout(3000).readTimeout(3000));
                if (response.getStatus() == 200 && response.getBody() != null) {
                    merge(groups, new String(response.getBody(), StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
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

        httpResponse.contentType("text/plain; version=0.0.4; charset=utf-8").sendFinish(result.toString());
    }

    private void merge(Map<String, MetricGroup> groups, String text) {
        for (String line : text.split("\n")) {
            if (line.startsWith("# HELP ")) {
                String rest = line.substring(7).trim();
                int space = rest.indexOf(' ');
                String name = space >= 0 ? rest.substring(0, space) : rest;
                MetricGroup group = groups.get(name);
                if (group == null) {
                    group = new MetricGroup();
                    group.metricName = name;
                    group.help = space >= 0 ? rest.substring(space + 1) : name;
                    groups.put(name, group);
                }
            } else if (line.startsWith("#")) {
                continue;
            } else if (!line.isEmpty()) {
                int brace = line.indexOf('{');
                String name = brace >= 0 ? line.substring(0, brace) : line;
                MetricGroup group = groups.get(name);
                if (group == null) {
                    group = new MetricGroup();
                    group.metricName = name;
                    group.help = name;
                    groups.put(name, group);
                }
                group.lines.add(line);
            }
        }
    }

    private void collect(Map<String, MetricGroup> groups, String instanceId, String appCode, AppStub stub) {
        for (Model model : stub.getAppMeta().getApp().models) {
            if (!hasMonitorAction(model)) continue;

            RequestImpl request = new RequestImpl();
            request.setInstance(instanceId);
            request.setApp(appCode);
            request.setModel(model.code);
            request.setAction(Monitor.ACTION_CODE_MONITOR);
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
            if (Monitor.ACTION_CODE_MONITOR.equals(action.code)) return true;
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
