package qingzhou.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.DataStore;
import qingzhou.api.Lang;
import qingzhou.engine.ModuleContext;
import qingzhou.registry.AppInfo;
import qingzhou.registry.MenuInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class AppContextImpl implements AppContext {
    private final ModuleContext moduleContext;
    private final AppInfo appInfo;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final I18nTool i18nTool = new I18nTool();

    private DataStore dataStore = new MemoryDataStore();
    private File appTemp;

    AppContextImpl(ModuleContext moduleContext, AppInfo appInfo) {
        this.moduleContext = moduleContext;
        this.appInfo = appInfo;
    }

    @Override
    public synchronized File getTemp() {
        if (appTemp == null) {
            appTemp = moduleContext.getTemp();
        }
        return appTemp;
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
        this.i18nTool.addI18n(key, i18n);
    }

    public String getI18n(String key, Lang lang, Object... args) {
        return this.i18nTool.getI18n(lang, key);
    }

    @Override
    public void addMenu(String menuName, String[] menuI18n, String menuIcon, int menuOrder) {
        this.appInfo.getMenuInfos().add(new MenuInfo(menuName, menuI18n, menuIcon, menuOrder));
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        actionFilters.add(actionFilter);
    }

    List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
