package qingzhou.agent.embedded.i18n;

public class I18nServiceImpl implements I18nService {

    @Override
    public String getI18n(String[] i18n, String lang, Object... args) {
        if (i18n == null || i18n.length == 0) return "";
        if (lang == null || lang.isEmpty()) {
            String result = i18n[0];
            for (int i = 1; i < i18n.length; i++) {
                result = result + " " + i18n[i];
            }
            return result;
        }
        for (String item : i18n) {
            if (item.startsWith(lang + ":")) {
                String result = item.substring(lang.length() + 1);
                if (args != null && args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        result = result.replace("{" + i + "}", String.valueOf(args[i]));
                    }
                }
                return result;
            }
        }
        return i18n[0];
    }
}