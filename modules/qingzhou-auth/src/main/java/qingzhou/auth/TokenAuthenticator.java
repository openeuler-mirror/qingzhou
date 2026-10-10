package qingzhou.auth;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.auth.AuthResult;
import qingzhou.http.server.auth.Authenticator;

@Component
public class TokenAuthenticator implements Authenticator {
    private static final String BEARER_PREFIX = "Bearer ";

    @Reference
    private TokenService tokenService;

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String header = request.getHeader("Authorization");
        // RFC 6750：scheme 不区分大小写（部分客户端发小写 bearer）
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return AuthResult.reject("token missing");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (tokenService.isRevoked(token)) return AuthResult.reject("token revoked");
        Object[] userRoles = tokenService.verifyToken(token);
        if (userRoles == null) return AuthResult.reject("invalid token");
        return AuthResult.pass((String) userRoles[0], (String[]) userRoles[1]);
    }
}
