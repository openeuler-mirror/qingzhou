package qingzhou.engine;

import java.io.File;

public interface ModuleContext {
    File getLibDir();

    <T> RegistryKey registerService(Class<T> clazz, T service);

    void unregisterService(RegistryKey registryKey);

    <T> T getService(Class<T> serviceType);

    File getInstanceDir();

    File getTemp();
}
