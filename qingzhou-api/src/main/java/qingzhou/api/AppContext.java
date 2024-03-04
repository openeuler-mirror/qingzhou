package qingzhou.api;

import qingzhou.api.metadata.AppMetadata;

import java.io.File;
import java.util.Collection;

public interface AppContext {
    AppMetadata getAppMetadata();

    String getPlatformName();

    String getPlatformVersion();

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    File getTemp();

    void addActionFilter(ActionFilter actionFilter);

    void addI18n(String key, String[] i18n);

    void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder);

    void setDefaultDataStore(DataStore dataStore);

    DataStore getDefaultDataStore();
}
