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

    private String verifyToken(String token) {
        return null;// todo
    }

    @Override
    public String[] excludedPaths() {
        return PasswordLogin.EXCLUDED_PATHS;
    }
}
