package qingzhou.framework.api;

public interface ConsoleContext {
    void setZhTrMap(char zh, char tr);

    void addI18N(String key, String[] i18n);

    String getI18N(String key, Object... args);

    void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder);

    MenuInfo getMenuInfo(String menuName);
}
