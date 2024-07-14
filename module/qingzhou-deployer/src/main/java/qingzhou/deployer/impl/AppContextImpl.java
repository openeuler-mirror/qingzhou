package qingzhou.deployer.impl;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Lang;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.ModuleContext;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.AppInfo;
import qingzhou.registry.MenuInfo;
import qingzhou.servlet.ServletService;
import qingzhou.ssh.SSHService;

import java.io.File;
import java.util.*;

class AppContextImpl implements AppContext {
    private final Class<?>[] serviceTypes = {CryptoService.class, Http.class, Json.class, Logger.class, QrGenerator.class, ServletService.class, SSHService.class};
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
    public <T> T getService(Class<T> clazz) {
        if (Arrays.stream(serviceTypes).anyMatch(aClass -> aClass == clazz)) {
            return moduleContext.getService(clazz);
        }
        throw new UnsupportedOperationException("No such service: " + clazz);
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return new ArrayList<>(Arrays.asList(serviceTypes));
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        actionFilters.add(actionFilter);
    }

    List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
