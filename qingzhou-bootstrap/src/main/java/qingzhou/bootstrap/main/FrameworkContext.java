package qingzhou.bootstrap.main;

import java.io.File;
import java.util.Collection;

public interface FrameworkContext {
    boolean isMaster();

    //  Qingzhou 产品名词
    String getName();

    // 产品版本信息
    String getVersion();

    File getDomain();

    File getTemp(String subName);

    File getLib();

    // 注意：此处注册的服务，会通过 AppContext 开放给所有应用，须确保服务是无状态的
    <T> RegistryKey registerService(Class<T> clazz, T service);

    void unregisterService(RegistryKey registryKey);

    Collection<Class<?>> getServiceTypes();

    <T> T getService(Class<T> serviceType);
}
