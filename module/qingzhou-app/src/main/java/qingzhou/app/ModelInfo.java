package qingzhou.app;

import qingzhou.api.ModelBase;
import qingzhou.api.metadata.ModelData;

import java.io.Serializable;
import java.util.*;

public class ModelInfo implements Serializable {
    public final ModelData model;
    public final Map<String, FieldInfo> fieldInfoMap;
    public final Map<String, ActionInfo> actionInfoMap;

    private final transient ModelBase instance;

    public ModelInfo(ModelData model, List<FieldInfo> fieldInfoMap, Collection<ActionInfo> actionInfoMap, ModelBase instance) {
        this.model = model;

        Map<String, FieldInfo> fieldInfoTemp = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : fieldInfoMap) {
            fieldInfoTemp.put(fieldInfo.fieldName, fieldInfo);
        }
        this.fieldInfoMap = Collections.unmodifiableMap(fieldInfoTemp);

        Map<String, ActionInfo> actionInfoTemp = new LinkedHashMap<>();
        for (ActionInfo actionInfo : actionInfoMap) {
            String actionName = actionInfo.modelAction.name();
            ActionInfo already = actionInfoTemp.put(actionName, actionInfo);
            if (already != null) {
                throw new IllegalArgumentException("Duplicate action name: " + actionName);
            }
        }
        this.actionInfoMap = Collections.unmodifiableMap(actionInfoTemp);

        this.instance = instance;
    }

    public ModelBase getInstance() {
        return instance;
    }
}
