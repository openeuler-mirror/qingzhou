package qingzhou.api;

import java.io.File;
import java.util.List;

/**
 * 轻舟提供的应用上下文接口，定义了应用运行时可用的基本功能。
 * 通过该接口，应用可以获取路径、注册监听和服务，以及获取客户端语言等。
 */
public interface AppContext {
    QingzhouApp getAppInfo();

    String getVersion();

    File getBase();

    File getTemp();

    String getPid();

    <T> T getService(Class<T> clazz);

    <T> T getService(Class<T> clazz, String name);

    List<ActionFilter> getActionFilters();

    <T, R> SharedFunctionRegistration registerSharedFunction(String name, SharedFunction<T, R> function);

    <T, R> SharedFunction<T, R> getSharedFunction(String name);

    <T> T getObjectInstance(Class<T> clazz);
}