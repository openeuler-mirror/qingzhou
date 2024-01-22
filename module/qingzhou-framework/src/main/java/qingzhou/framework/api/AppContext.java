package qingzhou.framework.api;

import java.io.File;
import java.util.Set;

public interface AppContext {
    Logger getLogger();

    File getCache();

    File getDomain();

    File getHome();

    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();
}
