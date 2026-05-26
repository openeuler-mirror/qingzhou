package qingzhou.api;

public enum Lang {
    zh("zh", "简体"),
    tr("tr", "繁體"),
    en("en", "English");

    public static final char SEPARATOR = ':';

    public final String flag;
    public final String info;

    Lang(String flag, String info) {
        this.flag = flag;
        this.info = info;
    }
}