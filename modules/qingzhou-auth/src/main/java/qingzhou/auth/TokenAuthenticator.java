package qingzhou.auth;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;

@Component
public class TokenAuthenticator implements Authenticator {
    @Reference
    private TokenService tokenService;

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String header = request.getHeader("Authorization");
        String BEARER = "Bearer ";
        if (header == null || !header.startsWith(BEARER)) {
            return AuthResult.reject("token missing");
        }
        Object[] userRoles = tokenService.verifyToken(header.substring(BEARER.length()).trim());
        if (userRoles == null) return AuthResult.reject("invalid token");
        return AuthResult.pass((String) userRoles[0], (String[]) userRoles[1]);
    }
}
