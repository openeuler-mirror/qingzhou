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
        String user = tokenService.verifyToken(header.substring(BEARER.length()).trim());
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid token");
    }

    @Override
    public String[] excludedPaths() {
        return LoginHandler.EXCLUDED_PATHS;
    }
}
