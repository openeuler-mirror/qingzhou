package qingzhou.api;

/**
 * Qingzhou 应用抽象类，定义了应用的启动和停止操作。
 */
public interface QingzhouApp {
    /**
     * 在轻舟实例每次启动时候执行一次
     * 通过控制台启动应用时候执行一次
     * 通过控制台部署应用时候执行一次
     */
    void start(AppContext appContext) throws Exception;

    /**
     * 通过控制台停止应用时候执行一次
     * 通过控制台卸载应用时候执行一次
     */
    default void stop(AppContext appContext) {
        // 默认实现为空，子类可以根据需要重写此方法以执行停止逻辑
    }

    /**
     * 通过控制台部署应用时候执行一次
     */
    default void install(AppContext appContext) throws Exception {
        // 默认实现为空，子类可以根据需要重写此方法以完成应用的安装
    }

    /**
     * 通过控制台卸载应用时候执行一次
     */
    default void uninstall(AppContext appContext) {
        // 默认实现为空，子类可以根据需要重写此方法以完成应用的卸载
    }
}
