package qingzhou.http.server;

public interface Authenticator {

    /**
     * 认证请求：凭据有效返回 pass；凭据无效返回 reject；
     */
    AuthResult authenticate(HttpRequest request);
}
