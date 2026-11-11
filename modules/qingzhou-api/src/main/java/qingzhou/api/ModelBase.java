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

    protected void setAppContext(AppContext appContext) {
        if (this.appContext != null) return;
        this.appContext = appContext;
    }

    protected void setCurrentRequest(Request request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    protected void clearCurrentRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }
}