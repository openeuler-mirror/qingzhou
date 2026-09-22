package qingzhou.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.api.FieldType;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=/monitor", HttpHandler.HANDLE_NO_AUTH + "=true"})
public class Monitor implements HttpHandler {
    @Reference
    private Registry registry;
    @Reference
    private Json json;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Map<String, Object> data = collect();
        httpResponse.contentType("application/json; charset=utf-8").sendFinish(json.toJson(data));
    }

    public Map<String, Object> collect() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String appCode : registry.getAllLocalApps()) {
            AppStubLocal stub = registry.getLocalApp(appCode);
            if (stub == null) continue;
            Map<String, Map<String, String>> appData = new LinkedHashMap<>();
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

                Map<String, String> modelData = new LinkedHashMap<>();
                for (ModelField field : model.fields) {
                    if (field.field_type != FieldType.monitor) continue;
                    String value = monitorData.get(field.code);
                    if (value == null) continue;
                    modelData.put(field.code, value);
                }
                if (!modelData.isEmpty()) {
                    appData.put(model.code, modelData);
                }
            }
            if (!appData.isEmpty()) {
                result.put(appCode, appData);
            }
        }
        return result;
    }

    private boolean hasMonitorAction(Model model) {
        for (ModelAction action : model.actions) {
            if (qingzhou.api.action.Monitor.ACTION_CODE_MONITOR.equals(action.code)) return true;
        }
        return false;
    }
}
