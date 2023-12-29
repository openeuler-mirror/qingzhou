package qingzhou.api;

public abstract class QingZhouApp {
    protected final AppContext appContext;

    protected QingZhouApp(AppContext appContext) {
        this.appContext = appContext;
    }

    public void init() throws Exception {
    }

    public abstract void start() throws Exception;

    public void stop() throws Exception {
    }

    public void destroy() throws Exception {
    }
}
