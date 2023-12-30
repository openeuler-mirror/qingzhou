package qingzhou.framework.impl.model;

import qingzhou.framework.api.MonitoringField;

import java.lang.reflect.Field;

public class MonitoringFieldInfo {
    public final MonitoringField monitoringField;
    public final Field field;

    MonitoringFieldInfo(MonitoringField monitoringField, Field field) {
        this.monitoringField = monitoringField;
        this.field = field;
    }
}