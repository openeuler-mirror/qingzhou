package qingzhou.framework.api;

public interface ConsoleContext extends AppStub {
    void addI18N(String key, String[] i18n);

    void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder);
}
