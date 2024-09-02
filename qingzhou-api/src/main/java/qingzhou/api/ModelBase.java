package qingzhou.api;

/**
 * 所有 Model 的基类。
 */
public abstract class ModelBase {
    protected AppContext appContext; // 注入关系对象，由平台调用

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void start() {
    }

    public void stop() {
    }
}