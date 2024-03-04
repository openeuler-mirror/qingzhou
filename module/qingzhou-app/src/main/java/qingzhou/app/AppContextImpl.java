package qingzhou.app;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.bootstrap.main.FrameworkContext;
import qingzhou.framework.InternalService;
import qingzhou.api.metadata.AppMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AppContextImpl implements AppContext {
    private final FrameworkContext frameworkContext;
    private final AppMetadataImpl appMetadata;
    private DataStore dataStore;
    private List<ActionFilter> actionFilters;

    public AppContextImpl(FrameworkContext frameworkContext) {
        this.frameworkContext = frameworkContext;
        this.appMetadata = new AppMetadataImpl();
    }

    @Override
    public File getTemp() {
        return frameworkContext.getTemp(appMetadata.getName());
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return frameworkContext.getServiceManager().getServiceTypes().stream().filter(aClass -> !InternalService.class.isAssignableFrom(aClass)).collect(Collectors.toList());
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        T service = frameworkContext.getServiceManager().getService(serviceType);
        if (service instanceof InternalService) {
            return null;
        }
        return service;
    }

    @Override
    public String getPlatformName() {
        return frameworkContext.getName();
    }

    @Override
    public String getPlatformVersion() {
        return frameworkContext.getVersion();
    }

    @Override
    public void setDefaultDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public DataStore getDefaultDataStore() {
        return dataStore;
    }

    @Override
    public void addI18n(String key, String[] i18n) {
        appMetadata.addI18n(key, i18n);
    }

    @Override
    public void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        appMetadata.addMenu(menuName, menuI18n, menuIcon, menuOrder);
    }

    public AppMetadata getAppMetadata() {
        return appMetadata;
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        if (actionFilters == null) {
            actionFilters = new ArrayList<>();
        }
        actionFilters.add(actionFilter);
    }

    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
