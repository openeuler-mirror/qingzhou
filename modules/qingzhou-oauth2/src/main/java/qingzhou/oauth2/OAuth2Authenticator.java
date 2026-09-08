package qingzhou.oauth2;

import org.osgi.service.component.annotations.Component;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;

@Component
public class OAuth2Authenticator implements Authenticator {
    @Override
    public AuthResult authenticate(HttpRequest request) {
        String cookie = getCookie(request.getHeader("Cookie"));
        if (cookie == null) {
            return AuthResult.reject("token missing");
        }
        String user = OAuth2CallbackHandler.verifyToken(cookie);
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid session");
    }

    private String getCookie(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(OAuth2CallbackHandler.COOKIE_NAME + "="))
                return trimmed.substring(OAuth2CallbackHandler.COOKIE_NAME.length() + 1);
        }
        return null;
    }
}
