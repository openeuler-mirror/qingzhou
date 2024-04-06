package qingzhou.engine;

import java.io.File;
import java.util.Collection;

public interface ModuleContext {
    String getName();

    String getVersion();

    File getLibDir();

    <T> RegistryKey registerService(Class<T> clazz, T service);

    void unregisterService(RegistryKey registryKey);

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    File getInstanceDir();

    File getTemp();
}
