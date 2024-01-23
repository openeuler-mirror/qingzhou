package qingzhou.app.impl;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelInfo implements Serializable {
    public final Model model;
    public final Map<String, FieldInfo> fieldInfoMap;
    public final Map<String, ActionInfo> actionInfoMap;
    public final String className;
    private Class<?> clazz;
    private ModelBase instance;

    public ModelInfo(Model model, List<FieldInfo> fieldInfoMap, List<ActionInfo> actionInfoMap, String className) {
        this.model = model;
        Map<String, FieldInfo> fieldInfoTemp = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : fieldInfoMap) {
            fieldInfoTemp.put(fieldInfo.fieldName, fieldInfo);
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
        this.actionInfoMap = Collections.unmodifiableMap(actionInfoTemp);
        this.className = className;
    }

    public void setModelClass(Class<?> clazz) {
        if (this.clazz != null) throw new IllegalStateException();
        this.clazz = clazz;
    }

    public void setModelInstance(ModelBase instance) {
        if (this.instance != null) throw new IllegalStateException();
        this.instance = instance;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public ModelBase getInstance() {
        return instance;
    }
}
