package qingzhou.api;

import java.io.File;

public interface AppContext {
    String getAppName();

    File getTemp();

    void setDefaultDataStore(DataStore dataStore);

    DataStore getDefaultDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();

    // 轻舟产品名词
    String getPlatformName();

    // 产品版本信息
    String getPlatformVersion();
}
