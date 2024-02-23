package qingzhou.app.impl;

import qingzhou.api.ModelField;

import java.lang.reflect.Field;

public class FieldInfo {
    public final ModelField modelField;
    public final String fieldName;
    private Field field;

    public FieldInfo(ModelField modelField, String fieldName) {
        this.modelField = modelField;
        this.fieldName = fieldName;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        if (this.field != null) throw new IllegalStateException();
        this.field = field;
    }
}
