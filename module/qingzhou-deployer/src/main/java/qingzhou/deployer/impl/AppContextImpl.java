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
    private final ModuleContext moduleContext;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final I18nTool i18nTool = new I18nTool();
    private final AppImpl app;

    private File appTemp;
    private File appDir;

    private Request currentRequest;

    AppContextImpl(ModuleContext moduleContext, AppImpl app) {
        this.moduleContext = moduleContext;
        this.app = app;
    }

    @Override
    public Request getCurrentRequest() {
        return currentRequest;
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

    @Override
    public String getI18n(Lang lang, String key, Object... args) {
        return this.i18nTool.getI18n(lang, key, args);
    }

    @Override
    public String getI18n(String key, Object... args) {
        return getI18n(getCurrentRequest().getLang(),
                key, args);
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    @Override
    public Menu addMenu(String name, String[] i18n) {
        MenuInfo newMenuInfo = new MenuInfo(name, i18n);
        app.getAppInfo().addMenuInfo(newMenuInfo);
        return new MenuImpl(newMenuInfo);
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return getServiceTypes().contains(clazz) ? moduleContext.getService(clazz) : null;
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        Set<Class<?>> types = new HashSet<>();
        Class<?>[] scopedTypes = {CryptoService.class, Http.class, Json.class, Logger.class, QrGenerator.class, ServletService.class, SSHService.class};
        for (Class<?> serviceType : scopedTypes) {
            if (moduleContext.getService(serviceType) != null) {
                types.add(serviceType);
            }
        }
        return types;
    }

    @Override
    public void callDefaultAction(Request request) throws Exception {
        app.invokeDefault(request);
    }

    @Override
    public String getModel(ModelBase modelBase) {
        for (Map.Entry<String, ModelBase> e : app.getModelBaseMap().entrySet()) {
            if (e.getValue() == modelBase) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public String getPlatformVersion() {
        return moduleContext.getPlatformVersion();
    }

    @Override
    public void addActionFilter(ActionFilter actionFilter) {
        actionFilters.add(actionFilter);
    }

    List<ActionFilter> getActionFilters() {
        return actionFilters;
    }
}
