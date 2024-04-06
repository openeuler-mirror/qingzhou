package qingzhou.app;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.api.metadata.AppMetadata;
import qingzhou.engine.ModuleContext;
import qingzhou.framework.InternalService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AppContextImpl implements AppContext {
    private final ModuleContext moduleContext;
    private final AppMetadataImpl appMetadata;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private DataStore dataStore = new MemoryDataStore();

    public AppContextImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
        this.appMetadata = new AppMetadataImpl();
    }

    @Override
    public File getTemp() {
        return moduleContext.getTemp();
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return moduleContext.getServiceTypes().stream().filter(aClass -> !InternalService.class.isAssignableFrom(aClass)).collect(Collectors.toList());
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        T service = moduleContext.getService(serviceType);
        if (service instanceof InternalService) {
            return null;
        }
        return service;
    }

    @Override
    public String getPlatformName() {
        return moduleContext.getName();
    }

    @Override
    public String getPlatformVersion() {
        return moduleContext.getVersion();
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
        actionFilters.add(actionFilter);
    }

    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
