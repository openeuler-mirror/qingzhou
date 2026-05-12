package qingzhou.registry;

public interface I18nService {
    String getI18n(String[] i18n, Object... args);

    String getI18n(String[] i18n, String lang, Object... args);
}
