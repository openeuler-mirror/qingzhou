package qingzhou.framework.api;

import java.util.Set;

public interface AppContext {
    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    void setDataStore(DataStore dataStore);

    DataStore getDataStore();

    void addActionFilter(ActionFilter actionFilter);

    ConsoleContext getConsoleContext();
}
