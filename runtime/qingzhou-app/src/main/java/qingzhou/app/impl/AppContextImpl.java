package qingzhou.app.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.api.metadata.AppMetadata;
import qingzhou.engine.ModuleContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
