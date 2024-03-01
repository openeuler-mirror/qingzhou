package qingzhou.bootstrap.main.impl;

import qingzhou.bootstrap.main.service.RegistryKey;
import qingzhou.bootstrap.main.service.ServiceManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceManagerImpl implements ServiceManager {
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<RegistryKey, Class<?>> registry = new HashMap<>();

    @Override
    public synchronized <T> RegistryKey registerService(Class<T> clazz, T service) {
        if (services.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        services.put(clazz, service);
        RegistryKey registryKey = new RegistryKey() {
        };
        registry.put(registryKey, clazz);
        return registryKey;
    }

    @Override
    public synchronized void unregisterService(RegistryKey registryKey) {
        Class<?> removed = registry.remove(registryKey);
        if (removed != null) {
            services.remove(removed);
        }
    }

    @Override
    public <T> T getService(Class<T> serviceType) {
        return (T) services.get(serviceType);
    }

    @Override
    public Collection<Class<?>> getServiceTypes() {
        return services.keySet();
    }
}
