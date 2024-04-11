package qingzhou.app.impl;


import java.io.Serializable;

public class FieldInfo implements Serializable {
    public final String fieldName;
    public final ModelFieldDataImpl modelField;
    public final String defaultValue;

    public FieldInfo(String fieldName, ModelFieldDataImpl modelField, String defaultValue) {
        this.fieldName = fieldName;
        this.modelField = modelField;
        this.defaultValue = defaultValue;
    }
}
