package qingzhou.engine;

import java.io.File;

public interface ModuleContext {
    File getLibDir();

    File getInstanceDir();

    File getTemp();

    <T> void registerService(Class<T> clazz, T service);
}
