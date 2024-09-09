package qingzhou.engine.impl.core;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class ModuleContextImpl implements ModuleContext {
    private final String name;
    private final ModuleInfo moduleInfo;
    final Map<Class<?>, Object> registeredServices = new HashMap<>();
    final Map<Class<?>, Object> injectedServices = new HashMap<>();

    private File temp;

    ModuleContextImpl(String name, ModuleInfo moduleInfo) {
        this.name = name;
        this.moduleInfo = moduleInfo;
    }

    @Override
    public ClassLoader getLoader() {
        return moduleInfo.getLoader();
    }

    @Override
    public String getPlatformVersion() {
        return getLibDir().getName().substring("version".length());
    }

    @Override
    public File getLibDir() {
        return moduleInfo.getEngineContext().getLibDir();
    }

    @Override
    public <T> void registerService(Class<T> serviceType, T service) {
        if (registeredServices.containsKey(serviceType)) {
            throw new IllegalStateException("Re-registration is not allowed: " + serviceType.getName());
        }
        registeredServices.put(serviceType, service);
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        T selfService = (T) registeredServices.get(clazz);
        if (selfService != null) return selfService;

        return (T) injectedServices.get(clazz);
    }

    @Override
    public File getInstanceDir() {
        return moduleInfo.getEngineContext().getInstanceDir();
    }

    @Override
    public File getTemp() {
        if (temp == null) {
            temp = FileUtil.newFile(moduleInfo.getEngineContext().getTemp(), name);
            FileUtil.mkdirs(temp);
        }
        return temp;
    }
}
