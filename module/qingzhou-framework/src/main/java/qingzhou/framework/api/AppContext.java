package qingzhou.framework.api;

import java.io.File;
import java.util.Collection;

public interface AppContext {
    File getTemp(String subName);

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();

    // 轻舟产品名词
    String getPlatformName();

    // 产品版本信息
    String getPlatformVersion();
}
