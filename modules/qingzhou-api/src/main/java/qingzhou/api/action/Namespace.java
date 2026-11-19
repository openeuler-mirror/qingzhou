package qingzhou.api.action;

/**
 * 命名空间切换的能力，对应内置动作 {@code namespace} 与 {@code current}。
 * <p>
 * 用于一个模块承载多个业务域（租户、集群、实例）的场景：{@link #namespace(String)} 切换当前生效空间，
 * {@link #current()} 报告当前空间。模块同时实现两者、并具备 {@code id=true} 与 {@code search=true} 字段时，
 * 管控台才渲染空间切换器。当前空间是模块级共享状态，切换对所有并发请求生效，需自行保证可见性与一致性。
 */
public interface Namespace {
    void namespace(String id) throws Exception;

    /**
     * @return 当前空间标识，用于回显；返回 {@code null} 将上报为空串
     */
    String current();
}
