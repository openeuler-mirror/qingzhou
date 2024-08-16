package qingzhou.api;

/**
 * QingZhou 应用抽象类，定义了应用的启动和停止操作。
 */
public interface QingzhouApp {

    /**
     * 启动应用。
     *
     * @param appContext 应用上下文，提供启动时所需环境和配置。
     * @throws Exception 启动过程中遇到的任何异常。
     */
    void start(AppContext appContext) throws Exception;

    /**
     * 停止应用。
     * 执行必要的清理工作，释放资源。
     *
     * @throws Exception 停止过程中遇到的任何异常。
     */
    default void stop() throws Exception {
        // 默认实现为空，子类可以根据需要重写此方法以执行停止逻辑。
    }
}

