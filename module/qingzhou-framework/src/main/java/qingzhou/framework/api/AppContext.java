package qingzhou.framework.api;

import java.io.File;
import java.util.Collection;

public interface AppContext {
    String getAppName();

    File getTemp();

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    void setDefaultDataStore(DataStore dataStore);

    DataStore getDefaultDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();

    // 轻舟产品名词
    String getPlatformName();

    // 产品版本信息
    String getPlatformVersion();
}
