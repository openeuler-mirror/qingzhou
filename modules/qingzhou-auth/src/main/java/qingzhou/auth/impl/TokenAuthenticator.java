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

    @Override
    public String[] excludedPaths() {
        return PasswordLoginHandler.EXCLUDED_PATHS;
    }
}
