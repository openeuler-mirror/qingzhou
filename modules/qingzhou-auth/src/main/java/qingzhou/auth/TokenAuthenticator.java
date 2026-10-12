package qingzhou.auth;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.auth.AuthResult;
import qingzhou.http.server.auth.Authenticator;

@Component
public class TokenAuthenticator implements Authenticator {
    @Reference
    private TokenService tokenService;

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String token = tokenService.retrieveToken(request);
        if (token == null) return AuthResult.reject("token missing");

        Object[] userRoles = tokenService.verifyToken(token);
        if (userRoles == null) return AuthResult.reject("invalid token");

        return AuthResult.pass((String) userRoles[0], (String[]) userRoles[1]);
    }
}
