package qingzhou.registry;

import java.util.Map;

import qingzhou.api.Lang;

public interface I18nService {
    Map<Lang, String> parse(String[] i18n);

    String getI18n(String[] i18n, Lang lang, Object... args);

    String getI18n(String[] i18n, String lang, Object... args);
}
