package qingzhou.registry;

public class MonitorFieldInfo {
    private String name;
    private String[] nameI18n;
    private String[] infoI18n;

    private boolean dynamic;

    private boolean dynamicMultiple;

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isDynamicMultiple() {
        return dynamicMultiple;
    }

    public void setDynamicMultiple(boolean dynamicMultiple) {
        this.dynamicMultiple = dynamicMultiple;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getNameI18n() {
        return nameI18n;
    }

    public void setNameI18n(String[] nameI18n) {
        this.nameI18n = nameI18n;
    }

    public String[] getInfoI18n() {
        return infoI18n;
    }

    public void setInfoI18n(String[] infoI18n) {
        this.infoI18n = infoI18n;
    }
}
