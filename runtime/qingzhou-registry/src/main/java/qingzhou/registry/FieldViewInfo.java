package qingzhou.registry;

import qingzhou.api.FieldType;

public class FieldViewInfo {
    public String group;
    public FieldType type;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }
}
