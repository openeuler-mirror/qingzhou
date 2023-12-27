package qingzhou.framework.impl.app.model;

import qingzhou.api.console.MonitoringField;

import java.lang.reflect.Field;

public class MonitoringFieldInfo {
    public final MonitoringField monitoringField;
    public final Field field;

    MonitoringFieldInfo(MonitoringField monitoringField, Field field) {
        this.monitoringField = monitoringField;
        this.field = field;
    }
}