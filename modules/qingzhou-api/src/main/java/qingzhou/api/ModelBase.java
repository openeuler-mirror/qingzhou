package qingzhou.api;

/**
 * 轻舟所有的模块，都需要用 @qingzhou.api.Model 标注，并且都需要从此类继承。
 */
public abstract class ModelBase implements QingzhouModel {
    private static final ThreadLocal<Request> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    private AppContext appContext;

    public AppContext getAppContext() {
        return appContext;
    }

    public void setAppContext(AppContext appContext) {
        if (this.appContext != null) return;
        this.appContext = appContext;
    }

    public Request getCurrentRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    public void setCurrentRequest(Request request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    // 请求处理结束必须清除：线程池复用线程时若不 remove，会残留上一个会话的 Request 引用（会话泄漏）
    public void clearCurrentRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }

    public void start() {
    }

    public void stop() {
    }
}