package qingzhou.framework.api;

import java.io.File;
import java.util.Set;

public interface AppContext {
    File getDomain();

    File getCache();

    File getHome();

    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    String getAppName();

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();

    ModelManager getModelManager();
}
