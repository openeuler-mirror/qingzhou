package qingzhou.monitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import qingzhou.api.FieldType;
import qingzhou.api.action.Monitor;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.dto.meta.annotation.ModelField;
import qingzhou.registry.AppStub;

/**
 * 采集单个应用下所有 monitor 字段：遍历含 monitor 动作且含 monitor 字段的模型，
 * 调用其 monitor 动作并取出字段值。供监控端点复用，不污染 Registry 契约。
 */
final class MonitorCollector {
    private MonitorCollector() {
    }

    static List<MonitorField> collectFields(String instanceId, String appCode, AppStub stub) {
        List<MonitorField> fields = new ArrayList<>();
        AppMeta appMeta = stub.getAppMeta();
        if (appMeta == null || appMeta.getApp() == null) return fields;

        for (Model model : appMeta.getApp().models) {
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

            Map<String, String> monitorData = (Map<String, String>) data;
            for (ModelField field : model.fields) {
                if (field.field_type != FieldType.monitor) continue;

                String value = monitorData.get(field.code);
                if (value != null) fields.add(new MonitorField(model.code, field, value));
            }
        }
        return fields;
    }

    static boolean hasMonitorAction(Model model) {
        for (ModelAction action : model.actions) {
            if (Monitor.ACTION_CODE_MONITOR.equals(action.code)) return true;
        }
        return false;
    }

    static final class MonitorField {
        final String modelCode;
        final ModelField field;
        final String value;

        MonitorField(String modelCode, ModelField field, String value) {
            this.modelCode = modelCode;
            this.field = field;
            this.value = value;
        }
    }
}
