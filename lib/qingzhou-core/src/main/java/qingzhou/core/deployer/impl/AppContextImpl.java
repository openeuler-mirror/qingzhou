package qingzhou.core.deployer.impl;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import qingzhou.api.*;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.I18nTool;
import qingzhou.core.registry.MenuInfo;
import qingzhou.engine.Service;

public class AppContextImpl implements AppContext {
    private final AppManagerImpl app;
    private final I18nTool i18nTool = new I18nTool();
    private File appTemp;

    AppContextImpl(AppManagerImpl app) {
        this.app = app;
    }

    @Override
    public Properties getAppProperties() {
        return app.getAppProperties();
    }

    @Override
    public Request getThreadLocalRequest() {
        return app.getThreadLocalRequest();
    }

    @Override
    public void setThreadLocalRequest(Request request) {
        app.setThreadLocalRequest(request);
    }

    @Override
    public File getAppDir() {
        return app.getAppDir();
    }

    @Override
    public synchronized File getTemp() {
        if (appTemp == null) {
            appTemp = new File(app.getModuleContext().getTemp(), app.getAppInfo().getName());
        }
        return appTemp;
    }

    @Override
    public void addI18n(String key, String[] i18n) {
        i18nTool.addI18n(key, i18n);
    }

    @Override
    public String getI18n(Lang lang, String key, Object... args) {
        return i18nTool.getI18n(lang, key, args);
    }

    @Override
    public String getI18n(String key, Object... args) {
        Lang lang = DeployerConstants.SESSION_LANG.get();
        return getI18n(lang, key, args);
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
            return app.getModuleContext().getService(clazz);
        } else {
            throw new IllegalArgumentException("No service available for " + clazz);
        }
    }

    public Collection<Class<?>> getServiceTypes() {
        return app.getModuleContext().getAvailableServiceTypes().stream().filter(aClass -> {
            Service annotation = aClass.getAnnotation(Service.class);
            return annotation == null || annotation.shareable();
        }).sorted(Comparator.comparing(o -> o.getAnnotation(Service.class).name())).collect(Collectors.toList());
    }

    @Override
    public void invokeSuperAction(Request request) throws Exception {
        app.invokeSuperAction(request);
    }

    @Override
    public String getPlatformVersion() {
        return app.getModuleContext().getPlatformVersion();
    }

    @Override
    public String[] getStartArgs() {
        return app.getModuleContext().getStartArgs();
    }

    @Override
    public void addAppActionFilter(ActionFilter... actionFilter) {
        app.addAppActionFilter(actionFilter);
    }

    @Override
    public void addModelActionFilter(ModelBase modelBase, ActionFilter... actionFilter) {
        app.addModelActionFilter(modelBase, actionFilter);
    }

    @Override
    public void setAuthAdapter(AuthAdapter authAdapter) {
        app.setAuthAdapter(authAdapter);
    }
    
    @Override
    public List<ActionFilter> getAppActionFilter() {
        return app.getAppActionFilter();
    }
}
