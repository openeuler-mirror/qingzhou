package qingzhou.framework.impl;

import qingzhou.framework.AppInfoManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.impl.app.AppInfoManagerImpl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrameworkContextImpl implements FrameworkContext {
    static FrameworkContextImpl instance;

    public static FrameworkContext getInstance() {
        return instance;
    }

    private final AppInfoManagerImpl appInfoManager = new AppInfoManagerImpl();
    private final Map<Class<?>, Object> services = new HashMap<>();

    @Override
    public AppInfoManager getAppInfoManager() {
        return appInfoManager;
    }

    @Override
    public <T> void registerService(Class<T> clazz, T service) {
        if (services.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        services.put(clazz, service);
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return (T) services.get(serviceType);
    }

    @Override
    public File getHome() {
        return ServerUtil.getHome();
    }

    @Override
    public File getDomain() {
        return ServerUtil.getDomain();
    }

    @Override
    public File getCache() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String timeFlag = df.format(new Date());
        return ServerUtil.getCache(timeFlag);
    }

    @Override
    public File getLib() {
        return ServerUtil.getLib();
    }

    @Override
    public Set<Class<?>> getServiceTypes() {
        return services.keySet();
    }
}
