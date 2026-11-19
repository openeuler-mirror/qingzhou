package qingzhou.api;

/**
 * 动作级拦截器，用于审计、限流、上下文注入等横切逻辑，通过 {@link AppContext#addActionFilter(ActionFilter...)} 注册。
 * <p>
 * 位置固定在平台内置校验之后、目标动作之前；不调用 {@link FilterChain#doFilter()} 即中断请求（可用于拒绝访问）。
 * 抛出的异常原样上抛，由框架统一转 500 并记录，不要吞掉。
 */
public interface ActionFilter {
    void doFilter(Request request, FilterChain chain) throws Throwable;
}
