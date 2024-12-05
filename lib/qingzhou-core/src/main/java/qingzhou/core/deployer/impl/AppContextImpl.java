package qingzhou.core.deployer.impl;

import qingzhou.api.*;
import qingzhou.core.registry.MenuInfo;
import qingzhou.engine.Service;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Collectors;

public class AppContextImpl implements AppContext {
    private final AppImpl app;

    AppContextImpl(AppImpl app) {
        this.app = app;
    }

    @Override
    public Properties getAppProperties() {
        return app.getAppProperties();
    }

    @Override
    public Request getCurrentRequest() {
        return app.getThreadLocalRequest();
    }

    @Override
    public File getAppDir() {
        return app.getAppDir();
    }

    @Override
    public synchronized File getTemp() {
        return app.getAppTemp();
    }

    @Override
    public void addI18n(String key, String[] i18n) {
        app.getI18nTool().addI18n(key, i18n);
    }

    @Override
    public String getI18n(Lang lang, String key, Object... args) {
        return app.getI18nTool().getI18n(lang, key, args);
    }

    @Override
    public String getI18n(String key, Object... args) {
        return getI18n(getCurrentRequest().getLang(),
                key, args);
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

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return app.getModuleContext().getAvailableServiceTypes().stream().filter(aClass -> {
            Service annotation = aClass.getAnnotation(Service.class);
            return annotation == null || annotation.shareable();
        }).sorted(Comparator.comparing(o -> o.getAnnotation(Service.class).name())).collect(Collectors.toList());
    }

    @Override
    public void invokeSuperAction(Request request) throws Exception {
        app.invokeDefault(request);
    }

    @Override
    public String getPlatformVersion() {
        return app.getModuleContext().getPlatformVersion();
    }

    @Override
    public void setActionFilter(ActionFilter actionFilter) {
        app.setAppActionFilter(actionFilter);
    }

    @Override
    public void setAuthAdapter(AuthAdapter authAdapter) {
        app.setAuthAdapter(authAdapter);
    }
}
