package qingzhou.http.server;

public interface Authenticator {

    /**
     * 认证请求：凭据有效返回 pass；凭据无效返回 reject；
     */
    AuthResult authenticate(HttpRequest request);

    /**
     * 本认证器声明无需认证的路径前缀（如登录端点、OAuth2 回调端点），由 HttpServer 在认证前统一豁免。
     */
    default String[] excludedPaths() {
        return null;
    }
}
