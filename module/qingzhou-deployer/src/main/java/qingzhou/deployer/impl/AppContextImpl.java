package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.I18nTool;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.http.Http;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.qr.QrGenerator;
import qingzhou.registry.MenuInfo;
import qingzhou.servlet.ServletService;
import qingzhou.uml.Uml;

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
        if (getServiceTypes().contains(clazz)) { // 安全拦截，防止调用到系统私有服务
            return moduleContext.getService(clazz);
        } else {
            throw new IllegalArgumentException("No service available for " + clazz);
        }
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        Class<?>[] injectedServices = {CryptoService.class, Http.class, Json.class, Logger.class, QrGenerator.class, ServletService.class, Uml.class};
        Set<Class<?>> types = new HashSet<>(Arrays.asList(injectedServices));
        File pluginsDir = FileUtil.newFile(moduleContext.getLibDir(), "plugins");
        return types;
    }

    @Override
    public void invokeSuperAction(Request request) throws Exception {
        app.invokeDefault(request);
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
