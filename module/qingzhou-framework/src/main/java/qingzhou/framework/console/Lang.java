package qingzhou.framework.console;

import qingzhou.framework.impl.CharMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Lang {
    zh("简体", "zh"),
    tr("繁體", "tr"),
    en("English", "en");

    private final String fullName;
    private final String flag;

    Lang(String fullName, String flag) {
        this.fullName = fullName;
        this.flag = flag;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getFlag() {
        return flag;
    }

    public boolean isZH() {
        return this == zh || this == tr;
    }

    public static Map<Lang, String> parseI18n(String[] i18n) {
        Map<Lang, String> i18nMap = new HashMap<>();
        for (String langLine : i18n) {
            if (langLine.contains("'")) {
                throw new UnsupportedOperationException(langLine);
            }

            Lang lang = null;
            int valueIndex = 0;
            if (langLine.length() > 2 && langLine.charAt(2) == ':') {
                lang = Lang.valueOf(langLine.substring(0, 2));
                valueIndex = 2 + 1;
            }
            if (lang == null) {
                lang = Lang.zh;
            }

            String val = langLine.substring(valueIndex);
            val = val.trim();
            // 防止漏写 en 等 i18n
            if (val.isEmpty()) {
                new IllegalArgumentException("Missing i18n of " + lang.name() + " for: " + Arrays.toString(i18n)).printStackTrace();
            }

            String origin = i18nMap.put(lang, val);
            if (origin != null) {
                new IllegalArgumentException("Duplicate i18n: " + Arrays.toString(i18n)).printStackTrace();
            }

            // 自动补充 繁体
            if (lang == Lang.zh) {
                i18nMap.put(Lang.tr, CharMap.zh2tr(val));
            }
        }

        return i18nMap;
    }
}
