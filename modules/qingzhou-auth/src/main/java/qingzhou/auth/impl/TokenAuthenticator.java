package qingzhou.auth.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.TokenService;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpAuthenticator;
import qingzhou.http.server.HttpRequest;

@Component
public class TokenAuthenticator implements HttpAuthenticator {
    @Reference
    private TokenService tokenService;

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String header = request.getHeader("Authorization");
        String BEARER = "Bearer ";
        if (header == null || !header.startsWith(BEARER)) {
            return AuthResult.missing(); // 无 Bearer 凭据为中性结果，交由其他认证器（如 OAuth2）决定
        }
        String user = tokenService.verifyToken(header.substring(BEARER.length()).trim());
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid token");
    }

<<<<<<< HEAD:modules/qingzhou-auth/src/main/java/qingzhou/auth/TokenAuthenticator.java
    /**
     * 校验 token，返回用户名；无效或已过期返回 null。
     */
    private String verifyToken(String token) {
        try {
            String payload = PasswordLogin.tokenCipher.decrypt(token);
            int sep = payload.lastIndexOf('|');
            return System.currentTimeMillis() < Long.parseLong(payload.substring(sep + 1))
                    ? payload.substring(0, sep) : null;
        } catch (Exception e) {
            return null;
        }
    }

=======
>>>>>>> 6a77da100e2512f761f030433cb058e6f5b2755c:modules/qingzhou-auth/src/main/java/qingzhou/auth/impl/TokenAuthenticator.java
    @Override
    public String[] excludedPaths() {
        return PasswordLoginHandler.EXCLUDED_PATHS;
    }
}
