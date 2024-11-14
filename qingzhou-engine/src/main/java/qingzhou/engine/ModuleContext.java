package qingzhou.engine;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public interface ModuleContext {
    Map<String, String> getConfig();

    ClassLoader getApiLoader();

    String getPlatformVersion();

    File getLibDir();

    File getInstanceDir();

    File getTemp();

    <T> void registerService(Class<T> serviceType, T serviceObj);

    /**
     * 获取本Module开放的Service，以及从其它Module注入的Service
     */
    <T> T getService(Class<T> serviceType);

    Collection<Class<?>> getAvailableServiceTypes();
}
