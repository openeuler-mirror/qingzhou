package qingzhou.engine.impl;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class ModuleContextImpl implements ModuleContext {
    private final EngineContext engineContext;
    final Map<Class<?>, Object> registeredServices = new HashMap<>();

    private File temp;

    ModuleContextImpl(EngineContext engineContext) {
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
    public File getInstanceDir() {
        return engineContext.getInstanceDir();
    }

    @Override
    public File getTemp() {
        if (temp == null) {
            temp = FileUtil.newFile(engineContext.getTemp(), UUID.randomUUID().toString());
            FileUtil.mkdirs(temp);
        }
        return temp;
    }
}
