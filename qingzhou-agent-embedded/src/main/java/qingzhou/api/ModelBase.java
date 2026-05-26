package qingzhou.api;

/**
 * 轻舟模块的抽象基类。
 * 应用中的每个功能模块都应该继承此类。
 */
public abstract class ModelBase implements QingzhouModel {
    private AppContext appContext;

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public boolean available() {
        return true;
    }

    public void start() throws Exception {
    }

    public void stop() {
    }
}