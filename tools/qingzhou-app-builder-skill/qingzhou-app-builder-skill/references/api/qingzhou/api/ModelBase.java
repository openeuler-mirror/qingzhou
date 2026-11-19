package qingzhou.api;

/**
 * 所有模块的基类，模块类须同时标注 {@code @qingzhou.api.annotations.Model}。
 * <p>
 * 平台按模块元数据为每个模块创建唯一实例（无参构造），注入 {@link AppContext}，
 * 待应用 {@link QingzhouApp#start(AppContext)} 返回后回调 {@link #start()}；停止时逆序回调 {@link #stop()}。
 * 实例是模块级单例，动作请求由线程池并发进入，模块内状态须自行保证线程安全。
 */
public abstract class ModelBase {
    private static final ThreadLocal<Request> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    private AppContext appContext;

    /**
     * 模块的运行期入口：实例根目录、应用配置与平台共享服务（日志、JSON、JDBC 等）均由此获取。
     */
    public AppContext getAppContext() {
        return appContext;
    }

    /**
     * 当前线程正在处理的请求。
     * <p>
     * 基于 {@link ThreadLocal} 实现，只在自定义 {@code @ModelAction} 方法执行期间有值；
     * 模块 start/stop 以及内置能力接口回调中为 {@code null}——后者所需的请求信息已由框架作为方法参数传入。
     */
    public Request getCurrentRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    /**
     * 模块初始化回调，所有模块均启动完成后应用才对外提供请求。
     * 适合在此读取配置、建立连接或注册 {@link ActionFilter}；只能抛运行时异常，否则导致应用启动失败。
     */
    public void start() {
    }

    /**
     * 模块停止回调。此路径中的异常不应阻断卸载流程。
     */
    public void stop() {
    }

    /**
     * 契约适配：模块与平台分属不同 OSGi bundle 且各自持有该基类，无包级可见通道，注入只能走 public；应用不得调用。
     */
    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    /**
     * 由框架在自定义动作执行前写入，应用不得调用。
     */
    public void setCurrentRequest(Request request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    /**
     * 由框架在动作结束后的 finally 中调用：线程池复用线程时若不清理，会残留上一个会话的 {@link Request} 引用。
     */
    public void clearCurrentRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }
}
