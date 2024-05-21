package qingzhou.engine.impl.core;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class ModuleContextImpl implements ModuleContext {
    private final String name;
    private final EngineContext engineContext;
    final Map<Class<?>, Object> registeredServices = new HashMap<>();
    final Map<Class<?>, Object> injectedServices = new HashMap<>();

    private File temp;

    ModuleContextImpl(String name, EngineContext engineContext) {
        this.name = name;
        this.engineContext = engineContext;
    }

    @Override
    public File getLibDir() {
        return engineContext.getLibDir();
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
        return engineContext.getInstanceDir();
    }

    @Override
    public File getTemp() {
        if (temp == null) {
            temp = Utils.newFile(engineContext.getTemp(), name);
            Utils.mkdirs(temp);
        }
        return temp;
    }
}
