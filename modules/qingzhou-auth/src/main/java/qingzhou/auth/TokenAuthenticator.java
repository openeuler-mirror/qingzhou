package qingzhou.auth;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpAuthenticator;
import qingzhou.http.server.HttpRequest;

@Component
public class TokenAuthenticator implements HttpAuthenticator {
    @Override
    public AuthResult authenticate(HttpRequest request) {
        String header = request.getHeader("Authorization");
        String BEARER = "Bearer ";
        if (header == null || !header.startsWith(BEARER)) {
            return AuthResult.missing(); // 无 Bearer 凭据为中性结果，交由其他认证器（如 OAuth2）决定
        }
        String user = verifyToken(header.substring(BEARER.length()).trim());
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid token");
    }

    /**
     * 校验 token，返回用户名；无效或已过期返回 null。
     */
    private String verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            int sep = payload.lastIndexOf('|');
            return System.currentTimeMillis() < Long.parseLong(payload.substring(sep + 1))
                    ? payload.substring(0, sep) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String[] excludedPaths() {
        return PasswordLogin.EXCLUDED_PATHS;
    }
}
