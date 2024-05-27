package qingzhou.api;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有 Model 的基类。
 */
public abstract class ModelBase {
    protected AppContext appContext; // 注入关系对象，由平台调用
    protected final List<ActionFilter> actionFilters = new ArrayList<>(); // 单个 Model 的 ActionFilter

    /**
     * 初始化函数，子类可以在此进行定制化初始化，例如 i18n 等。
     */
    public void start() {
    }

    // 页面表单字段分组信息
    public Groups groups() {
        return null;
    }

    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }

    // 指定此 Model 使用的 DataStore。轻舟会为每个 Model 设置一个默认实现，应用可通过覆写此方法实现自定义
    public DataStore getDataStore() {
        throw new IllegalStateException("Model " + this.getClass().getName() + " does not have " + DataStore.class.getSimpleName());
    }
}