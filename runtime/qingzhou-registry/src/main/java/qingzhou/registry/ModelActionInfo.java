package qingzhou.registry;

public class ModelActionInfo {
    public final String name;
    public final String[] nameI18n;
    public final String[] infoI18n;
    public final String effectiveWhen;
    public final boolean supportBatch;
    public final ActionPageInfo actionPageInfo;

    public ModelActionInfo(String name, String[] nameI18n, String[] infoI18n, String effectiveWhen, boolean supportBatch, ActionPageInfo actionPageInfo) {
        this.name = name;
        this.nameI18n = nameI18n;
        this.infoI18n = infoI18n;
        this.effectiveWhen = effectiveWhen;
        this.supportBatch = supportBatch;
        this.actionPageInfo = actionPageInfo;
    }
}
