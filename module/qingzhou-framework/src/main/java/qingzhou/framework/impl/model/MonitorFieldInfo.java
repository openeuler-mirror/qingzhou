package qingzhou.framework.impl.model;

import qingzhou.framework.api.MonitorField;

import java.lang.reflect.Field;

public class MonitorFieldInfo {
    public final MonitorField monitorField;
    public final Field field;

    MonitorFieldInfo(MonitorField monitorField, Field field) {
        this.monitorField = monitorField;
        this.field = field;
    }
}