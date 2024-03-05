package qingzhou.app;


import qingzhou.api.metadata.ModelFieldData;

import java.io.Serializable;
import java.lang.reflect.Field;

public class FieldInfo implements Serializable {
    public final ModelFieldData modelField;
    public final String fieldName;

    private transient Field field;

    public FieldInfo(ModelFieldData modelField, String fieldName) {
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
