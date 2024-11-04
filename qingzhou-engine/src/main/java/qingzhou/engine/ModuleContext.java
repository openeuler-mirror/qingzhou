package qingzhou.engine;

import java.io.File;
import java.util.Map;

public interface ModuleContext {
    Map<String, String> getConfig();

    ClassLoader getApiLoader();

    String getPlatformVersion();

    File getLibDir();

    File getInstanceDir();

    File getTemp();

    <T> void registerService(Class<T> clazz, T service);

    /**
     * 获取本Module开放的Service，以及从其它Module注入的Service
     */
    <T> T getService(Class<T> clazz);
}
