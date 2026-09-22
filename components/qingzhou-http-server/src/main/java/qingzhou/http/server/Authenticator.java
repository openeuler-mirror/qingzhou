package qingzhou.http.server;

public interface Authenticator {

    /**
     * 认证请求：凭据有效返回 {@link AuthResult#pass}；凭据无效返回 {@link AuthResult#reject}；
     * 本认证器不适用于该请求（如凭据类型不匹配）返回 {@link AuthResult#abstain()} 或 null，二者等价，会转交后续认证器。
     */
    AuthResult authenticate(HttpRequest request);
}
