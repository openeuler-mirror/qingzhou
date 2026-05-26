package qingzhou.api;

/**
 * ActionFilter接口定义了用于处理请求和响应的过滤器行为。
 */
public interface ActionFilter {
    String doFilter(Request request) throws Exception;
}