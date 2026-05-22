package qingzhou.api;

/**
 * 轻舟所有的模块，都需要用 @qingzhou.api.Model 标注，并且都需要从此类继承。
 */
public abstract class ModelBase implements QingzhouModel {
    private AppContext appContext;

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    public void setAppContext(AppContext appContext) {
        if (this.appContext != null) return;
        this.appContext = appContext;
    }

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void start() {
    }

    public void stop() {
    }
}