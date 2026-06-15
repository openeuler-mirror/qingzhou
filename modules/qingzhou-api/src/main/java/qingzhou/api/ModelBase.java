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

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void start() {
    }

    public void stop() {
    }
}