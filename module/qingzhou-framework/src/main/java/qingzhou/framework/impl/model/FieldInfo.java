package qingzhou.framework.impl.model;

import qingzhou.framework.api.ModelField;

import java.lang.reflect.Field;

public class FieldInfo {
    public final ModelField modelField;
    public final Field field;

    public FieldInfo(ModelField modelField, Field field) {
        this.modelField = modelField;
        this.field = field;
    }
}
