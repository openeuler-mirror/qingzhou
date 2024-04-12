package qingzhou.registry;

public class ModelActionInfo {
    public String name;
    public String[] nameI18n;
    public String[] infoI18n;
    public String effectiveWhen;
    public boolean supportBatch;
    public ActionPageInfo actionPageInfo;

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

    public String getEffectiveWhen() {
        return effectiveWhen;
    }

    public void setEffectiveWhen(String effectiveWhen) {
        this.effectiveWhen = effectiveWhen;
    }

    public boolean isSupportBatch() {
        return supportBatch;
    }

    public void setSupportBatch(boolean supportBatch) {
        this.supportBatch = supportBatch;
    }

    public ActionPageInfo getActionPageInfo() {
        return actionPageInfo;
    }

    public void setActionPageInfo(ActionPageInfo actionPageInfo) {
        this.actionPageInfo = actionPageInfo;
    }
}
