package qingzhou.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Lang;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.ModuleContext;
import qingzhou.registry.AppInfo;
import qingzhou.registry.MenuInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

class AppContextImpl implements AppContext {
    private final ModuleContext moduleContext;
    private final AppInfo appInfo;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final I18nTool i18nTool = new I18nTool();

    private File appTemp;
    private File appDir;

    AppContextImpl(ModuleContext moduleContext, AppInfo appInfo) {
        this.moduleContext = moduleContext;
        this.appInfo = appInfo;
    }

    @Override
    public File getAppDir() {
        return appDir;
    }

    public void setAppDir(File appDir) {
        this.appDir = appDir;
    }

    @Override
    public synchronized File getTemp() {
        if (appTemp == null) {
            appTemp = new File(moduleContext.getTemp(), appInfo.getName());
        }
        return appTemp;
    }

    @Override
    public void addI18n(String key, String[] i18n) {
        this.i18nTool.addI18n(key, i18n);
    }

    public String getI18n(Lang lang, String key, Object... args) {
        return this.i18nTool.getI18n(lang, key, args);
    }

    @Override
    public void addMenu(String name, String[] i18n, String icon, int order) {
        Collection<MenuInfo> menuInfos = this.appInfo.getMenuInfos();
        if (menuInfos == null) {
            menuInfos = new HashSet<>();
            this.appInfo.setMenuInfos(menuInfos);
        }
        menuInfos.add(new MenuInfo(name, i18n, icon, order));
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        actionFilters.add(actionFilter);
    }

    List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
