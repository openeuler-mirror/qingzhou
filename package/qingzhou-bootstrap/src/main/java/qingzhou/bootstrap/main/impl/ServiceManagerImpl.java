package qingzhou.bootstrap.main.impl;

import qingzhou.bootstrap.main.service.RegistryKey;
import qingzhou.bootstrap.main.service.ServiceListener;
import qingzhou.bootstrap.main.service.ServiceManager;

import java.util.*;

public class ServiceManagerImpl implements ServiceManager {
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<RegistryKey, Class<?>> registry = new HashMap<>();
    private final Set<ServiceListener> listeners = new HashSet<>();

    @Override
    public synchronized <T> RegistryKey registerService(Class<T> clazz, T service) {
        if (services.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        services.put(clazz, service);
        RegistryKey registryKey = new RegistryKey() {
        };
        registry.put(registryKey, clazz);
        listeners.forEach(serviceListener -> serviceListener.serviceRegistered(clazz));
        return registryKey;
    }

    @Override
    public synchronized void unregisterService(RegistryKey registryKey) {
        Class<?> removed = registry.remove(registryKey);
        if (removed != null) {
            Object service = services.remove(removed);
            listeners.forEach(serviceListener -> serviceListener.serviceUnregistered(removed, service));
        }
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        listeners.add(listener);
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
