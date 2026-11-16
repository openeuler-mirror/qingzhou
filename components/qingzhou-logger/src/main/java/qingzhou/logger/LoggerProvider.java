package qingzhou.logger;

/**
 * 按名称获取日志器。
 *
 * <p>命名日志器不作为 OSGi 服务注册，原因：
 * 多个同类型服务只能靠 service.ranking 选取，
 * 框架停止期默认日志器先注销时，无 target 的 {@code @Reference Logger} 会重绑到其余实例，
 * 日志随之流入其他 writer，会导致停止过程中，系统停止日志进入应用日志文件，以及控制台输出看到重新启动的日志内容。</p>
 */
public interface LoggerProvider {
    /**
     * @return 该名称的日志器；未配置该名称时返回默认日志器
     */
    Logger getLogger(String name);
}
