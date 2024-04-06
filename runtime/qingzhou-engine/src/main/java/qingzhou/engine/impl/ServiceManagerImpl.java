package qingzhou.engine.impl;

import qingzhou.engine.RegistryKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceManagerImpl {
    private final Map<Class<?>, Object> SERVICES = new HashMap<>();
    private final Map<RegistryKey, Class<?>> REGISTRY = new HashMap<>();

    public synchronized <T> RegistryKey registerService(Class<T> clazz, T service) {
        if (SERVICES.containsKey(clazz)) {
            throw new IllegalArgumentException();
        }
        SERVICES.put(clazz, service);
        RegistryKey registryKey = new RegistryKey() {
        };
        REGISTRY.put(registryKey, clazz);
        System.out.println("Registered Service: " + clazz.getName());
        return registryKey;
    }

    public synchronized void unregisterService(RegistryKey registryKey) {
        Class<?> removed = REGISTRY.remove(registryKey);
        if (removed != null) {
            SERVICES.remove(removed);
            System.out.println("Unregistered Service: " + removed.getName());
        }
    }

    public <T> T getService(Class<T> serviceType) {
        return (T) SERVICES.get(serviceType);
    }

    public Collection<Class<?>> getServiceTypes() {
        return SERVICES.keySet();
    }
}
