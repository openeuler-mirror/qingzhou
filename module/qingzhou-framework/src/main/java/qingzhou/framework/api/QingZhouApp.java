package qingzhou.framework.api;

public abstract class QingZhouApp {
    public abstract void start(AppContext appContext) throws Exception;

    public void stop() throws Exception {
    }
}
