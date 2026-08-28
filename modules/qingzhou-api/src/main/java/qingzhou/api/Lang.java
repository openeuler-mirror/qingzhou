package qingzhou.api;

public enum Lang {
    zh_Hans, zh_Hant, en;

    // i18n 文案中语言代码与文本的分隔符，如 "en:System Management"；无分隔符视为 zh_Hans。
    public static final char SEPARATOR = ':';
}
