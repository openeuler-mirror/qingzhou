package qingzhou.agent.embedded.i18n;

public interface I18nService {
    String getI18n(String[] i18n, String lang, Object... args);
}