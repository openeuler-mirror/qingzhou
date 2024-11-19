package qingzhou.engine.impl;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class ModuleContextImpl implements ModuleContext {
    private final ModuleInfo moduleInfo;

    Object config;
    ClassLoader apiLoader;
    final Map<Class<?>, Object> registeredServices = new HashMap<>();
    final Map<Class<?>, Object> injectedServices = new HashMap<>();

    private File temp;

    ModuleContextImpl(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
    }

    @Override
    public Object getConfig() {
        return config;
    }

    @Override
    public ClassLoader getApiLoader() {
        return apiLoader;
    }

    @Override
    public String getPlatformVersion() {
        return getLibDir().getName().substring("version".length());
    }

    @Override
    public File getLibDir() {
        return moduleInfo.engineContext.getLibDir();
    }

    @Override
    public <T> void registerService(Class<T> serviceType, T serviceObj) {
        if (registeredServices.containsKey(serviceType)) {
            throw new IllegalStateException("Re-registration is not allowed: " + serviceType.getName());
        }
        registeredServices.put(serviceType, serviceObj);
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        Object injected = injectedServices.get(serviceType);
        if (injected != null) {
            return (T) injected;
        }

        Object registered = registeredServices.get(serviceType);
        if (registered != null) {
            return (T) registered;
        }

        return null;
    }

    @Override
    public Collection<Class<?>> getAvailableServiceTypes() {
        return new HashSet<Class<?>>() {{
            addAll(registeredServices.keySet());
            addAll(injectedServices.keySet());
        }};
    }

    @Override
    public File getInstanceDir() {
        return moduleInfo.engineContext.getInstanceDir();
    }

    @Override
    public File getTemp() {
        if (temp == null) {
            temp = FileUtil.newFile(moduleInfo.engineContext.getTemp(), moduleInfo.getName());
            FileUtil.mkdirs(temp);
        }
        return temp;
    }
}
