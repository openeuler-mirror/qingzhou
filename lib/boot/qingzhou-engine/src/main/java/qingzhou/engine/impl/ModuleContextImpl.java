package qingzhou.engine.impl;

import java.io.File;
import java.util.*;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.ServiceListener;
import qingzhou.engine.util.FileUtil;

class ModuleContextImpl implements ModuleContext {
    private final ModuleInfo moduleInfo;

    Object config;
    ClassLoader apiLoader;
    final Map<Class<?>, Object> registeredServices = new HashMap<>();
    final Map<Class<?>, Object> injectedServices = new HashMap<>();
    final Set<ServiceListener> serviceListeners = new HashSet<>();

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

        notifyListeners(ServiceListener.ServiceEvent.REGISTERED, serviceType);
    }

    private void notifyListeners(ServiceListener.ServiceEvent event, Class<?> serviceType) {
        moduleInfo.engineContext.moduleInfoList.forEach(moduleInfo -> moduleInfo.moduleContext.serviceListeners.forEach(serviceListener -> serviceListener.onServiceEvent(event, serviceType)));
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        T found = getService0(serviceType);
        if (found != null) {
            notifyListeners(ServiceListener.ServiceEvent.GOT, serviceType);
        }
        return found;
    }

    public <T> T getService0(Class<T> serviceType) {
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
    public void addServiceListener(ServiceListener listener) {
        serviceListeners.add(listener);
    }

    @Override
    public String[] getStartArgs() {
        String[] startArgs = moduleInfo.engineContext.startArgs;
        if (startArgs != null) {
            return Arrays.copyOfRange(startArgs, 0, startArgs.length);
        }
        return new String[0];
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
