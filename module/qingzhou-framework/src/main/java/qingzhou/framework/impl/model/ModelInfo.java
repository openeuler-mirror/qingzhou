package qingzhou.framework.impl.model;

import qingzhou.framework.api.Model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelInfo {
    public final Model model;
    public final Map<String, FieldInfo> fieldInfoMap;
    public final Map<String, MonitoringFieldInfo> monitoringFieldInfoMap;
    public final Map<String, ActionInfo> actionInfoMap;
    public final Class<?> clazz;

    public ModelInfo(Model model, List<FieldInfo> fieldInfoMap, List<MonitoringFieldInfo> monitoringFieldInfoMap, List<ActionInfo> actionInfoMap, Class<?> clazz) {
        this.model = model;
        Map<String, FieldInfo> fieldInfoTemp = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : fieldInfoMap) {
            fieldInfoTemp.put(fieldInfo.field.getName(), fieldInfo);
        }
        Map<String, MonitoringFieldInfo> monitoringFieldInfoTemp = new LinkedHashMap<>();
        for (MonitoringFieldInfo monitoringFieldInfo : monitoringFieldInfoMap) {
            monitoringFieldInfoTemp.put(monitoringFieldInfo.field.getName(), monitoringFieldInfo);
        }
        Map<String, ActionInfo> actionInfoTemp = new LinkedHashMap<>();
        for (ActionInfo actionInfo : actionInfoMap) {
            String actionName = actionInfo.modelAction.name();
            ActionInfo already = actionInfoTemp.put(actionName, actionInfo);
            if (already != null) {
                throw new IllegalArgumentException("Duplicate action name: " + actionName);
            }
        }

        this.fieldInfoMap = Collections.unmodifiableMap(fieldInfoTemp);
        this.monitoringFieldInfoMap = Collections.unmodifiableMap(monitoringFieldInfoTemp);
        this.actionInfoMap = Collections.unmodifiableMap(actionInfoTemp);
        this.clazz = clazz;
    }
}
