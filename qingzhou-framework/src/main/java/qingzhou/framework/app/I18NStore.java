package qingzhou.framework.app;

import qingzhou.api.Lang;
import qingzhou.framework.util.StringUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class I18NStore {
    private final Map<String, String[]> langMap = new HashMap<>();

    public void addI18N(String key, String[] i18n, boolean checkContainChinese) {
        Map<Lang, String> i18nMap = retrieveI18n(i18n);
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

    public static Map<Lang, String> retrieveI18n(String[] i18n) {
        Map<Lang, String> i18nMap = new HashMap<>();
        for (String langLine : i18n) {
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
        }

        // 自动补充 繁体
        if (!i18nMap.containsKey(Lang.tr)) {
            String zhI18n = i18nMap.get(Lang.zh);
            if (zhI18n != null) {
                i18nMap.put(Lang.tr, CharMap.zh2tr(zhI18n));
            }
        }

        return i18nMap;
    }
}
