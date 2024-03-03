package qingzhou.api;

import qingzhou.api.type.Showable;

public abstract class ModelBase implements Showable {
    private AppContext appContext;

    // 由框架注入
    public void setAppContext(AppContext appContext) {
        if (this.appContext != null) throw new IllegalStateException(); // 不需重复设置，AppContext 应是不变量
        this.appContext = appContext;
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public DataStore getDataStore() {
        return getAppContext().getDefaultDataStore();
    }

    // 添加 i18n 等定制初始化
    public void init() {
    }

    // 对于创建时候未传入 id 参数的，如 master 的 App，可以通过此方法计算 id，以进行查重等操作
    public String resolveId(Request request) {
        return null;
    }

    // 定制校验逻辑，返回 i18n 的 key
    public String validate(Request request, String fieldName) {
        return null;
    }

    public Groups groups() {
        return null;
    }

    public Options options(Request request, String fieldName) {
        return null;
    }
}
