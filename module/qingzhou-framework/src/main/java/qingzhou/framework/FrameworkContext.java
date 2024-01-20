package qingzhou.framework;

import qingzhou.framework.api.Logger;

import java.io.File;
import java.util.Set;

public interface FrameworkContext {
    String LOCAL_NODE_NAME = "local";
    String MASTER_APP_NAME = "master";
    String NODE_APP_NAME = "node";

    boolean isMaster();

    Logger getLogger();

    AppManager getAppManager();

    Set<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);

    File getCache();

    File getCache(File parent);

    File getDomain();

    File getHome();

    File getLib();

    // 注意：此处注册的服务，会通过 AppContext 开放给所有应用，须确保服务是无状态的
    <T> void registerService(Class<T> clazz, T service);

    void addServiceListener(ServiceListener listener);
}
