package qingzhou.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpAuthenticator;
import qingzhou.http.server.HttpRequest;

@Component(configurationPid = "qingzhou-oauth2")
public class OAuth2Authenticator implements HttpAuthenticator {
    @Reference
    private Crypto crypto;

    private String authorizationEndpoint;
    private String clientId;
    private String redirectUri;
    private String scope;

    private Cipher cipher;
    private String[] excludedPaths;

    @Activate
    public void init(Map<String, String> config) throws Exception {
        authorizationEndpoint = config.get("authorize_endpoint");
        clientId = config.get("client_id");
        redirectUri = config.get("redirect_uri") + OAuth2Callback.EXCLUDED_CALLBACK_PATH;
        scope = config.get("scope");

        cipher = crypto.getCipher(crypto.generateKey());
    }

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String cookie = getCookie(request.getHeader("Cookie"));
        if (cookie == null) {
            String state = "0"; // 无状态设计，不可在单机上随机生成
            return AuthResult.challenge(buildAuthorizationUrl(state));
        }
        String user = verifySession(cookie);
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid session");
    }

    @Override
    public String[] excludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new String[]{OAuth2Callback.EXCLUDED_CALLBACK_PATH};
        }
        return excludedPaths;
    }

    private String buildAuthorizationUrl(String state) {
        StringBuilder url = new StringBuilder(authorizationEndpoint)
                .append("?response_type=code")
                .append("&client_id=").append(encode(clientId))
                .append("&redirect_uri=").append(encode(redirectUri))
                .append("&state=").append(encode(state));
        if (scope != null) url.append("&scope=").append(encode(scope));
        return url.toString();
    }

    private String verifySession(String token) {
        try {
            String payload = cipher.decrypt(token);
            int sep = payload.lastIndexOf('|');
            long expire = Long.parseLong(payload.substring(sep + 1));
            return System.currentTimeMillis() > expire ? null : payload.substring(0, sep);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCookie(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(OAuth2Callback.COOKIE_NAME + "="))
                return trimmed.substring(OAuth2Callback.COOKIE_NAME.length() + 1);
        }
        return null;
    }

    private String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
