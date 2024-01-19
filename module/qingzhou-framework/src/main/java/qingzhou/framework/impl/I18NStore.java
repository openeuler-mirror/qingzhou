package qingzhou.framework.impl;

import qingzhou.framework.console.Lang;
import qingzhou.framework.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class I18NStore {
    private final Map<String, String[]> langMap = new HashMap<>();

    public void addI18N(String key, String[] i18n, boolean checkContainChinese) {
        Map<Lang, String> i18nMap = Lang.retrieveI18n(i18n);
        for (Lang lang : i18nMap.keySet()) {
            String val = i18nMap.get(lang);
            String[] indexedData = langMap.computeIfAbsent(key, s -> new String[Lang.values().length]);
            String old = indexedData[lang.ordinal()];
            if (old == null) {
                indexedData[lang.ordinal()] = val;
            } else { // 提醒更正会被覆盖的key
                new IllegalArgumentException("Duplicate i18n key: " + key + ", old: " + old + ", new: " + val).printStackTrace();
            }
        }

        // 防止将英文写成中文的情况发生
        if (checkContainChinese) {
            String val = langMap.get(key)[Lang.en.ordinal()];
            if (StringUtil.containsZHChar(val)) {
                new IllegalArgumentException("Please do not use Chinese in English: " + key).printStackTrace();
            }
        }
    }

    public String getI18N(Lang lang, String key, Object... args) {
        String[] values = langMap.get(key);
        if (values != null) {
            String s = values[lang.ordinal()];
            if (s != null && args != null && args.length > 0) {
                return String.format(s, args);
            }
            return s;
        }
        return null;
    }
}
