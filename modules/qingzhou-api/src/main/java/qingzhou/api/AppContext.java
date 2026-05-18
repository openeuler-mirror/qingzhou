package qingzhou.api;

import java.io.File;
import java.util.Properties;

public interface AppContext {
    // 平台框架版本
    String getVersion();

    // 应用配置属性
    Properties getProperties();

    // 平台实例的根目录
    File getBase();

    // 应用专属的临时目录
    File getTemp();

    /**
     * 获取平台提供的共享服务，如：qingzhou.logger.Logger、qingzhou.json.Json等。
     */
    <T> T getService(Class<T> clazz);

    /**
     * 获取平台提供的指定名字的共享服务，如：qingzhou.jdbc.JdbcPool 的多实例服务。
     */
    <T> T getService(Class<T> clazz, String name);

    // 添加请求拦截器
    void addActionFilter(ActionFilter... actionFilter);

    /**
     * 获取应用内 @App @Model 注解的类实例化后的对象
     */
    <T> T getObjectInstance(Class<T> type);
}
