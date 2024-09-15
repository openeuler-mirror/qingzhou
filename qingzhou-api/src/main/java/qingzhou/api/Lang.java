package qingzhou.api;

/**
 * 语言枚举类，定义了支持的语言及其相关信息。
 */
public enum Lang {
    // 定义了三种语言：简体中文、英文、繁体中文
    zh("zh", "简体"), // zh代表中文，info为"简体"
    tr("tr", "繁體"), // tr代表繁体中文，info为"繁體"
    en("en", "English"); // en代表英文，info为"English"

    // 枚举常量之间的分隔符
    public static final char SEPARATOR = ':';

    // 每种语言的标志和相关信息
    public final String flag; // 语言的标志，如"zh"、"en"
    public final String info; // 语言的描述信息，如"简体"

    /**
     * 构造函数，用于初始化每种语言的标志和信息。
     *
     * @param flag 语言的标志字符串。
     * @param info 语言的描述信息。
     */
    Lang(String flag, String info) {
        this.flag = flag;
        this.info = info;
    }
}
