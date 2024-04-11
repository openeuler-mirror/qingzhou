package qingzhou.registry;

public class ModelFieldInfo {
    public final String name;
    public final String[] nameI18n;
    public final String[] infoI18n;
    public final boolean shownOnList;
    public final FieldValidationInfo fieldValidationInfo;
    public final FieldPageInfo fieldPageInfo;

    public ModelFieldInfo(String name, String[] nameI18n, String[] infoI18n, boolean shownOnList, FieldValidationInfo fieldValidationInfo, FieldPageInfo fieldPageInfo) {
        this.name = name;
        this.nameI18n = nameI18n;
        this.infoI18n = infoI18n;
        this.shownOnList = shownOnList;
        this.fieldValidationInfo = fieldValidationInfo;
        this.fieldPageInfo = fieldPageInfo;
    }
}
