package qingzhou.bootstrap.main.service;

import java.util.Collection;

public interface ServiceManager {
    // 注意：此处注册的服务，会通过 AppContext 开放给所有应用，须确保服务是无状态的
    <T> RegistryKey registerService(Class<T> clazz, T service);

    void unregisterService(RegistryKey registryKey);

    void addServiceListener(ServiceListener listener);

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);
}
