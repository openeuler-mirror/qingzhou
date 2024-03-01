package qingzhou.api;

public abstract class ModelBase implements ShowModel {
    private AppContext appContext;

    // 由框架注入
    public void setAppContext(AppContext appContext) {
        if (this.appContext != null) throw new IllegalStateException(); // 不需重复设置，AppContext 应是不变量
        this.appContext = appContext;
    }

    @Override
    public AppContext getAppContext() {
        return appContext;
    }

    // 添加 i18n 等定制初始化
    public void init() {
    }

    // 对于创建时候未传入 id 参数的，如 master 的 App，可以通过此方法计算 id，以进行必要的查重校验等操作
    public String resolveId(Request request) {
        return null;
    }

    // 定制校验逻辑，返回 i18n 的 key
    public String validate(Request request, String fieldName) {
        return null;
    }

    public Groups group() {
        return null;
    }

    public Options options(Request request, String fieldName) {
        return null;
    }
}
