package qingzhou.api;

public enum Lang {
    zh("zh", "简体"),
    en("en", "English"),
    tr("tr", "繁體");

    public final String flag;
    public final String info;

    Lang(String flag, String info) {
        this.flag = flag;
        this.info = info;
    }
}
