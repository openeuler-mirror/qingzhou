package qingzhou.api;

/**
 * 轻舟所有的模块，都需要用 @qingzhou.api.Model 标注，并且都需要从此类继承。
 */
public abstract class ModelBase {
    private AppContext appContext;

    // 获取 AppContext API 对象，该对象在整个应用内唯一
    protected final AppContext getAppContext() {
        return appContext;
    }

    // 获取当前模块的模块名，即 @qingzhou.api.Model 属性 code 的值
    protected final String getModel() {
        return appContext.getModel(this);
    }

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void start() {
    }

    public void stop() {
    }
}