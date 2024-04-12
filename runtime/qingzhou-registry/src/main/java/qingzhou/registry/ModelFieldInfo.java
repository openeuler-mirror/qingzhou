package qingzhou.registry;

public class ModelFieldInfo {
    public String name;
    public String[] nameI18n;
    public String[] infoI18n;
    public boolean shownOnList;
    public FieldValidationInfo fieldValidationInfo;
    public FieldPageInfo fieldPageInfo;

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

    public FieldPageInfo getFieldPageInfo() {
        return fieldPageInfo;
    }

    public void setFieldPageInfo(FieldPageInfo fieldPageInfo) {
        this.fieldPageInfo = fieldPageInfo;
    }
}
