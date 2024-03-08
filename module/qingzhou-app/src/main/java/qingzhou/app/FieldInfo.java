package qingzhou.app;


import qingzhou.api.metadata.ModelFieldData;

import java.io.Serializable;

public class FieldInfo implements Serializable {
    public final String fieldName;
    public final ModelFieldData modelField;
    private final String defaultValue;

    public FieldInfo(String fieldName, ModelFieldData modelField, String defaultValue) {
        this.fieldName = fieldName;
        this.modelField = modelField;
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
