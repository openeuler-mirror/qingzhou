package qingzhou.core.deployer;

import qingzhou.api.Lang;
import qingzhou.core.deployer.impl.Controller;
import qingzhou.engine.util.Utils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I18nTool {
    private final Map<String, String[]> langMap = new HashMap<>();

    public void addI18n(String key, String[] i18n) {
        addI18n(key, i18n, true);
    }

    public void addI18n(String key, String[] i18n, boolean checkContainChinese) {
        Map<Lang, String> i18nMap = retrieveI18n(i18n);
        for (Lang lang : i18nMap.keySet()) {
            String val = i18nMap.get(lang);
            String[] indexedData = langMap.computeIfAbsent(key, s -> new String[Lang.values().length]);
            String old = indexedData[lang.ordinal()];
            if (old == null) {
                indexedData[lang.ordinal()] = val;
            } else { // 提醒更正会被覆盖的key
                throw new IllegalArgumentException("Duplicate i18n key: " + key + ", old: " + old + ", new: " + val);
            }
        }

        // 防止将英文写成中文的情况发生
        if (checkContainChinese) {
            String val = langMap.get(key)[Lang.en.ordinal()];
            if (CharMap.containsZHChar(val)) {
                throw new IllegalArgumentException("Please do not use Chinese in English: " + key);
            }
        }
    }

    public String getI18n(Lang lang, String key, Object... args) {
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
            if (langLine.length() > 2 && langLine.charAt(2) == Lang.SEPARATOR) {
                lang = Lang.valueOf(langLine.substring(0, 2));
                valueIndex = 2 + 1;
            }
            if (lang == null) {
                lang = Lang.zh;
            }

            String val = langLine.substring(valueIndex);
            val = val.trim();
            if (val.contains("'")) {
                new UnsupportedOperationException("Single quotes (') are not supported: " + langLine).printStackTrace();
            }

            String origin = i18nMap.put(lang, val);
            if (origin != null) {
                throw new IllegalArgumentException("Duplicate i18n: " + Arrays.toString(i18n));
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

    private static class CharMap {
        private static final Map<Character, Character> ZH_TR_MAP = new HashMap<>();
        private static final Set<Character> DETECTED = new CopyOnWriteArraySet<>();

        static {
            try {
                Properties props = Utils.streamToProperties(CharMap.class.getResourceAsStream("/" + Controller.class.getPackage().getName().replace(".", "/") + "/CharMap.txt"));
                String zh = props.getProperty("zh");
                String tr = props.getProperty("tr");
                for (int i = 0; i < zh.length(); i++) {
                    Character check = ZH_TR_MAP.put(zh.charAt(i), tr.charAt(i));
                    if (check != null) {
                        throw new IllegalArgumentException("Please remove duplicate characters");
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * 字符串是否包含中文
         */
        static boolean containsZHChar(String str) {
            if (str == null) return false;
            str = str.trim();
            if (str.isEmpty()) return false;

            Pattern p = Pattern.compile("[\u4E00-\u9FA5\\！\\，\\。\\（\\）\\《\\》\\“\\”\\？\\：\\；\\【\\】]");
            Matcher m = p.matcher(str);
            return m.find();
        }

        static String zh2tr(String msg) {
            if (msg == null) return null;
            msg = msg.trim();
            if (msg.isEmpty()) return null;

            StringBuilder twMsg = new StringBuilder();
            for (int i = 0; i < msg.length(); i++) {
                char c = msg.charAt(i);
                Character twChar = ZH_TR_MAP.get(c);
                if (twChar == null) {
                    twChar = c;

                    // 记录，以更新繁体字的字典
                    if (containsZHChar(String.valueOf(c))) {
                        DETECTED.add(c);
                        StringBuilder needAdd = new StringBuilder();
                        for (Character character : DETECTED) {
                            needAdd.append(character);
                        }
                        System.out.println(Lang.tr.info + " char (" + c + ") not found for: " + msg);
                        System.out.println(Lang.tr.info + " chars not found: " + needAdd);
                    }
                }
                twMsg.append(twChar);
            }
            return twMsg.toString();
        }
    }
}
