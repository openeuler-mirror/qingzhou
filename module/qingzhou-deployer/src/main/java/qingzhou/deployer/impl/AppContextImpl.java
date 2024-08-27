package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.ModuleContext;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.MenuInfo;
import qingzhou.servlet.ServletService;
import qingzhou.ssh.SSHService;

import java.io.File;
import java.util.*;

class AppContextImpl implements AppContext {
    private final Class<?>[] serviceTypes = {CryptoService.class, Http.class, Json.class, Logger.class, QrGenerator.class, ServletService.class, SSHService.class};
    private final ModuleContext moduleContext;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final I18nTool i18nTool = new I18nTool();
    private final AppImpl app;

    private File appTemp;
    private File appDir;

    AppContextImpl(ModuleContext moduleContext, AppImpl app) {
        this.moduleContext = moduleContext;
        this.app = app;
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
            appTemp = new File(moduleContext.getTemp(), app.getAppInfo().getName());
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
        Collection<MenuInfo> menuInfos = this.app.getAppInfo().getMenuInfos();
        if (menuInfos == null) {
            menuInfos = new HashSet<>();
            this.app.getAppInfo().setMenuInfos(menuInfos);
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
    public void callDefaultAction(Request request, Response response) throws Exception {
        app.invokeDefault(request, response);
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        actionFilters.add(actionFilter);
    }

    List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
