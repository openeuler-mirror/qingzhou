package qingzhou.api;

/**
 * 平台提供接口，在应用内部 ModelAction 被调用过程中使用，用以与平台交互
 */
public interface AppContext extends BasicContext {
    void addActionFilter(ActionFilter... actionFilter);

    /**
     * 获取平台提供的共享服务，如：qingzhou.logger.Logger、qingzhou.json.Json等。
     */
    <T> T getService(Class<T> clazz);

    /**
     * 获取平台提供的指定名字的共享服务，如：qingzhou.jdbc.JdbcPool 的多实例服务。
     */
    <T> T getService(Class<T> clazz, String name);

    /**
     * 获取应用内 @App @Model 注解的类实例化后的对象
     */
    <T> T getObjectInstance(Class<T> type);
}
