package qingzhou.registry;

public class ModelFieldInfo {
    private String name;
    private String[] nameI18n;
    private String[] infoI18n;
    private String defaultValue;
    private boolean shownOnList;
    private FieldValidationInfo fieldValidationInfo;
    private FieldViewInfo fieldViewInfo;

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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isShownOnList() {
        return shownOnList;
    }

    public void setShownOnList(boolean shownOnList) {
        this.shownOnList = shownOnList;
    }

    public FieldValidationInfo getFieldValidationInfo() {
        return fieldValidationInfo;
    }

    public void setFieldValidationInfo(FieldValidationInfo fieldValidationInfo) {
        this.fieldValidationInfo = fieldValidationInfo;
    }

    public FieldViewInfo getFieldViewInfo() {
        return fieldViewInfo;
    }

    public void setFieldViewInfo(FieldViewInfo fieldViewInfo) {
        this.fieldViewInfo = fieldViewInfo;
    }
}
