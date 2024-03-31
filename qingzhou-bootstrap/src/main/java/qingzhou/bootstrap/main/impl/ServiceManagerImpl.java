package qingzhou.bootstrap.main.impl;

import qingzhou.bootstrap.main.service.RegistryKey;
import qingzhou.bootstrap.main.service.ServiceManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceManagerImpl implements ServiceManager {
    private final Map<Class<?>, Object> SERVICES = new HashMap<>();
    private final Map<RegistryKey, Class<?>> REGISTRY = new HashMap<>();

    @Override
    public synchronized <T> RegistryKey registerService(Class<T> clazz, T service) {
        if (SERVICES.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        SERVICES.put(clazz, service);
        RegistryKey registryKey = new RegistryKey() {};
        REGISTRY.put(registryKey, clazz);
        return registryKey;
    }

    @Override
    public synchronized void unregisterService(RegistryKey registryKey) {
        Class<?> removed = REGISTRY.remove(registryKey);
        if (removed != null) {
            SERVICES.remove(removed);
        }
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return (T) SERVICES.get(serviceType);
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return SERVICES.keySet();
    }
}
