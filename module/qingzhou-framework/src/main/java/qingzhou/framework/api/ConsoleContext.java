package qingzhou.framework.api;

public interface ConsoleContext {
    ModelManager getModelManager();

    String getI18N(Lang lang, String key, Object... args);

    MenuInfo getMenuInfo(String menuName);

    void addI18N(String key, String[] i18n);

    void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder);
}
