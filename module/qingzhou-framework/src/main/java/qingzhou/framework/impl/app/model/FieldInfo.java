package qingzhou.framework.impl.app.model;

import qingzhou.api.console.ModelField;

import java.lang.reflect.Field;

public class FieldInfo {
    public final ModelField modelField;
    public final Field field;

    public FieldInfo(ModelField modelField, Field field) {
        this.modelField = modelField;
        this.field = field;
    }
}
