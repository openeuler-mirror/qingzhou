package qingzhou.framework;

import java.io.File;
import java.util.Set;

public interface FrameworkContext {
    AppManager getAppInfoManager();

    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    File getCache();

    File getDomain();

    File getLib();

    File getHome();

    // （注意：此处注册的服务，会通过 AppContext 开放给所有应用，须确保服务是无状态的，像 SSH、Servlet 等内部服务也不请勿注册）
    <T> void registerService(Class<T> clazz, T service);
}
