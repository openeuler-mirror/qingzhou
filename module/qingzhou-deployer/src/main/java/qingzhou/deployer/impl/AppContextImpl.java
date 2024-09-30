package qingzhou.deployer.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.Lang;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
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

class AppContextImpl implements AppContext {
    private final ModuleContext moduleContext;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final I18nTool i18nTool = new I18nTool();
    private final AppImpl app;

    private File appTemp;
    private File appDir;

    private Lang requestLang;

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

    @Override
    public String getI18n(Lang lang, String key, Object... args) {
        return this.i18nTool.getI18n(lang, key, args);
    }

    @Override
    public String getI18n(String key, Object... args) {
        return getI18n(getRequestLang(), key, args);
    }

    @Override
    public Lang getRequestLang() {
        return requestLang;
    }

    public void setRequestLang(Lang requestLang) {
        this.requestLang = requestLang;
    }

    @Override
    public void addMenu(String name, String[] i18n, String icon, int order, String... parent) {
        MenuInfo newMenuInfo = new MenuInfo(name, i18n, icon, order);

        MenuInfo parentMenu = null;
        if (parent.length > 0) {
            parentMenu = app.getAppInfo().getMenuInfo(parent[parent.length - 1]);
        }

        if (parentMenu != null) {
            parentMenu.addChild(newMenuInfo);
        } else {
            app.getAppInfo().addMenuInfo(newMenuInfo);
        }
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
