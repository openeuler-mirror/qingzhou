package qingzhou.api;

/**
 * 轻舟所有的模块，都需要用 @qingzhou.api.annotations.Model 标注，并且都需要从此类继承。
 */
public abstract class ModelBase {
    private static final ThreadLocal<Request> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    private AppContext appContext;

    public AppContext getAppContext() {
        return appContext;
    }

    public Request getCurrentRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    public void start() {
    }

    public void stop() {
    }

    // 契约适配：应用模型与平台分属不同 OSGi bundle，无包级可见通道，注入必须走 public；应用不得调用
    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setCurrentRequest(Request request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    // 请求处理结束必须清除：线程池复用线程时若不 remove，会残留上一个会话的 Request 引用
    public void clearCurrentRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }
}