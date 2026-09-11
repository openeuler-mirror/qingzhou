package qingzhou.oauth2;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;

@Component
public class OAuth2Authenticator implements Authenticator {
    @Reference
    private OAuth2Handler oAuth2Handler;

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String token = oAuth2Handler.getCookie(request, OAuth2Handler.TOKEN_COOKIE_NAME);
        if (token == null) {
            return AuthResult.reject("token missing");
        }
        String user = oAuth2Handler.verifyToken(token);
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid session");
    }
}
