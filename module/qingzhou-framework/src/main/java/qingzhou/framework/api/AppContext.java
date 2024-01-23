package qingzhou.framework.api;

import java.io.File;

public interface AppContext {
    Logger getLogger();

    File getTemp(String subName);

    File getDomain();

    File getHome();

    <T> T getService(Class<T> serviceType);

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();
}
