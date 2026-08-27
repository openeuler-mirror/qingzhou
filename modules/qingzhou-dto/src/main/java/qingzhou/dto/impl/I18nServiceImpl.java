package qingzhou.dto.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.api.Lang;
import qingzhou.dto.I18nService;

@Component
public class I18nServiceImpl implements I18nService {
    private final Map<Character, Character> ZH_TR_MAP = new HashMap<>();

    @Activate
    public void init() {
        try (InputStream inputStream = I18nServiceImpl.class.getResourceAsStream("/" + I18nServiceImpl.class.getPackage().getName().replace(".", "/") + "/zh_Hant.txt")) {
            String zh = "";
            String zhHant = "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.startsWith(Lang.zh_Hans + "=")) {
                    zh = line;
                } else if (line.startsWith(Lang.zh_Hant + "=")) {
                    zhHant = line;
                }
            }
            for (int i = 0; i < zh.length(); i++) {
                Character check = ZH_TR_MAP.put(zh.charAt(i), zhHant.charAt(i));
                if (check != null) {
                    throw new IllegalArgumentException("Please remove duplicate characters");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getI18n(String[] i18n, Lang lang, Object... args) {
        if (lang == null) lang = Lang.zh_Hans;

        // 查找素材
        final String[] foundI18n = new String[1];
        final String[] zhI18nBak = new String[1];
        Lang finalLang = lang;
        visit(i18n, (currentLang, currentI18n) -> {
            if (currentLang == finalLang) {
                foundI18n[0] = currentI18n;
                return false;
            } else {
                if (finalLang == Lang.zh_Hant && currentLang == Lang.zh_Hans) {
                    zhI18nBak[0] = currentI18n;
                }
                return true;
            }
        });

        // 决策
        String i18nVal = null;
        if (foundI18n[0] != null) {
            i18nVal = foundI18n[0];
        } else if (zhI18nBak[0] != null) {
            i18nVal = zh2tr(zhI18nBak[0]);
        }

        // 组装结果
        if (i18nVal != null && args != null && args.length > 0) {
            return String.format(i18nVal, args);
        }
        return i18nVal;
    }

    @Override
    public String getI18n(String[] i18n, String lang, Object... args) {
        Lang toLang = null;
        try {
            if (lang != null && !lang.isEmpty())
                toLang = Lang.valueOf(lang);
        } catch (Exception ignored) {
        }
        return getI18n(i18n, toLang, args);
    }

    private String zh2tr(String msg) {
        if (msg == null) return null;
        msg = msg.trim();
        if (msg.isEmpty()) return null;

        StringBuilder twMsg = new StringBuilder();
        for (int i = 0; i < msg.length(); i++) {
            char zhChar = msg.charAt(i);
            Character trChar = ZH_TR_MAP.get(zhChar);
            if (trChar == null) {
                trChar = zhChar;
            }
            twMsg.append(trChar);
        }
        return twMsg.toString();
    }

    private void visit(String[] i18n, Visitor visitor) {
        if (i18n == null) return;

        for (String langLine : i18n) {
            Lang lang = null;
            int valueIndex = 0;
            int sep = langLine.indexOf(Lang.SEPARATOR);
            if (sep > 0) {
                try {
                    lang = Lang.valueOf(langLine.substring(0, sep));
                    valueIndex = sep + 1;
                } catch (IllegalArgumentException e) {
                    // 前缀不是语言代码，整行视为 zh 文案
                }
            }
            if (lang == null) {
                lang = Lang.zh_Hans;
            }

            String val = langLine.substring(valueIndex).trim();

            boolean continueVisit = visitor.visit(lang, val);
            if (!continueVisit) {
                break;
            }
        }
    }

    private interface Visitor {
        boolean visit(Lang currentLang, String currentI18n); // 返回 true 则继续访问，false 则终止
    }
}
