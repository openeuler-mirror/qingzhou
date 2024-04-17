package qingzhou.api;

/**
 * ActionFilter接口定义了用于处理请求和响应的过滤器行为。
 * 过滤器通常应用于预处理阶段，例如权限验证、参数校验等，
 */
public interface ActionFilter {

    /**
     * 执行过滤操作的方法。
     * 此方法会在请求处理前被调用，允许对传入的Request和Response对象进行修改，
     * 同时可以利用AppContext获取到应用程序上下文信息以辅助过滤逻辑的执行。
     *
     * @param request  框架的请求对象，包含请求参数等数据。
     * @param response 框架的返回给客户端的响应对象。
     * @return 返回错误信息，为null表示正常
     * @throws Exception 在执行过滤过程中若遇到异常情况，可以通过抛出Exception来通知上层处理。
     */
    String doFilter(Request request, Response response) throws Exception;
}
