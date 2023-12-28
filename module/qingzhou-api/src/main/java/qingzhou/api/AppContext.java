package qingzhou.api;

import qingzhou.api.console.ConsoleContext;

import java.io.File;
import java.util.Set;

public interface AppContext {
    File getDomain();

    File getTemp();

    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    ConsoleContext getConsoleContext();
}
