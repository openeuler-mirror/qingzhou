package qingzhou.api.console;

public interface ConsoleContext {
    ModelManager getModelManager();

    void addI18N(String key, String[] i18n);

    void addI18N(String key, String[] i18n, boolean checkContainChinese);

    String getI18N(String key, Object... args);

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void setMenuInfo(String menuName, String[] menuI18n, String menuIcon, int menuOrder);
}
