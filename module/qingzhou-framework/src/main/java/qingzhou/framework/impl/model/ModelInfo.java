package qingzhou.framework.impl.model;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelBase;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelInfo {
    public final Model model;
    public final Map<String, FieldInfo> fieldInfoMap;
    public final Map<String, ActionInfo> actionInfoMap;
    public final Class<?> clazz;
    public final ModelBase instance;

    public ModelInfo(Model model, List<FieldInfo> fieldInfoMap, List<ActionInfo> actionInfoMap, Class<?> clazz) {
        this.model = model;
        Map<String, FieldInfo> fieldInfoTemp = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : fieldInfoMap) {
            fieldInfoTemp.put(fieldInfo.field.getName(), fieldInfo);
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
        this.clazz = clazz;
        try {
            this.instance = (ModelBase) clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("The class annotated by the Model needs to have a public parameter-free constructor.", e);
        }
    }
}
