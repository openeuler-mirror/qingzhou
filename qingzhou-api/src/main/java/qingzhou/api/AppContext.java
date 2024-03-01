package qingzhou.api;

import java.io.File;

public interface AppContext {
    String getPlatformName();

    String getPlatformVersion();

    File getTemp();

    void addActionFilter(ActionFilter actionFilter);

    void setDefaultDataStore(DataStore dataStore);

    DataStore getDefaultDataStore();

    void addI18N(String key, String[] i18n);

    void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder);

    AppMetadata getMetadata(); // DTO，用于序列化

    // todo ?? 展开 ConsoleContext 里面的阿，不要这么多层次了?
    ConsoleContext getConsoleContext();
}
