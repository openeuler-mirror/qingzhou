package qingzhou.http.server;

/**
 * HTTP 请求认证器。
 * 实现类注册为 OSGi 服务后，由 HttpServer 在分发请求前统一调用。
 * 多认证器可并存（如 Bearer Token、OAuth2），组合规则见 {@link AuthResult}。
 */
public interface HttpAuthenticator {

    /**
     * 认证请求。
     * 凭据有效返回 pass；凭据无效返回 reject；请求未携带本认证器支持的凭据返回 missing（勿用 reject，否则会阻断其他认证器）；需重定向引导认证（如 OAuth2）返回 challenge。
     */
    AuthResult authenticate(HttpRequest request);

    /**
     * 本认证器声明无需认证的路径前缀（如登录端点、OAuth2 回调端点），由 HttpServer 在认证前统一豁免。
     */
    default String[] excludedPaths() {
        return null;
    }
}
