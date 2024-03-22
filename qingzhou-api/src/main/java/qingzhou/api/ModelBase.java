package qingzhou.api;

/**
 * ModelBase 是一个抽象类，作为模型的基础，提供了应用上下文的管理以及一些常用方法的定义。
 * 它实现了 Showable 接口，但具体展示逻辑需要在子类中实现。
 */
public abstract class ModelBase {
    // 应用上下文
    private AppContext appContext;

    /**
     * 设置应用上下文。
     * 该方法由框架调用，用于注入 AppContext 实例。
     * 不允许重复设置，即 AppContext 在对象生命周期内应保持不变。
     *
     * @param appContext 应用上下文实例。
     * @throws IllegalStateException 如果 appContext 已经被设置过，则抛出此异常。
     */
    public void setAppContext(AppContext appContext) {
        if (this.appContext != null) throw new IllegalStateException(); // 不需重复设置，AppContext 应是不变量
        this.appContext = appContext;
    }

    /**
     * 获取应用上下文。
     *
     * @return 返回应用上下文实例。
     */
    public AppContext getAppContext() {
        return appContext;
    }

    /**
     * 获取默认数据存储。
     *
     * @return 返回默认数据存储实例。
     */
    public DataStore getDataStore() {
        return getAppContext().getDefaultDataStore();
    }

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void init() {
    }

    /**
     * 定制验证逻辑，返回 i18n 的 key。
     *
     * @param request   请求实例。
     * @param fieldName 验证的字段名。
     * @return 返回验证失败时的 i18n 错误消息 key，如果验证通过则返回 null。
     */
    public String validate(Request request, String fieldName) {
        return null;
    }

    /**
     * 获取页面表单字段分组信息。
     *
     * @return 返回页面表单字段分组信息，如果无分组则返回 null。
     */
    public Groups groups() {
        return null;
    }

    /**
     * 获取字段选项。
     *
     * @param request   请求实例。
     * @param fieldName 相关字段名。
     * @return 返回字段的选项信息，如果无需选项或不存在则返回 null。
     */
    public Options options(Request request, String fieldName) {
        return null;
    }
}

