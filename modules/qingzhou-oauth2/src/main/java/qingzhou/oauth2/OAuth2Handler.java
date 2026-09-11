package qingzhou.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

@Component(configurationPid = "qingzhou-oauth2", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=/", HttpHandler.HANDLE_NO_AUTH + "=true"},
        service = {OAuth2Handler.class, HttpHandler.class})
public class OAuth2Handler implements HttpHandler {
    private static final String AUTHORIZE_PATH = "/oauth2/authorize";
    private static final String CALLBACK_PATH = "/oauth2/callback";

    private final String CSRF_COOKIE_NAME = "oauth2_csrf";
    static final String TOKEN_COOKIE_NAME = "oauth2_session";

    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;
    @Reference
    private Crypto crypto;

    private String authorizationEndpoint;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String scope;
    private String redirectUri;
    private String usernameField;

    private long tokenExpireMillis;
    private Cipher tokenCipher;
    private Cipher clientSecretCipher;

    @Activate
    public void init(Map<String, String> config) {
        authorizationEndpoint = config.get("authorize_endpoint");
        clientId = config.get("client_id");
        clientSecret = config.get("client_secret"); //安全考虑：此处不要解密
        tokenEndpoint = config.get("token_endpoint");
        userinfoEndpoint = config.get("userinfo_endpoint");
        usernameField = config.get("userinfo_username_field");
        scope = config.get("scope");
        redirectUri = config.get("redirect_uri") + CALLBACK_PATH;

        tokenExpireMillis = Integer.parseInt(config.getOrDefault("token_expire_seconds", "" + 30 * 60)) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
        clientSecretCipher = crypto.getGlobalCipher();
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String path = request.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // 精确匹配，避免 /x/oauth2/authorize 之类误命中
        if (path.equals(AUTHORIZE_PATH)) {
            String state = newState(); // state 随每次授权请求随机生成并存入 Cookie，回调时比对，借用此机制防登录 CSRF
            response.header("Set-Cookie", CSRF_COOKIE_NAME + "=" + state + "; Path=/; Max-Age=600; HttpOnly; Secure; SameSite=Lax")
                    .redirect(buildAuthorizationUrl(state));
        } else if (path.equals(CALLBACK_PATH)) {
            callback(request, response);
        } else {
            response.status400Finish();
        }
    }

    private String newState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    private String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void callback(HttpRequest request, HttpResponse response) throws Exception {
        String csrfToken = request.getParameter("state"); // OAUTH2 协议要求 state 必须传递
        if (csrfToken == null || !csrfToken.equals(getCookie(request, CSRF_COOKIE_NAME))) {
            response.status400Finish();
            return;
        }

        String code = request.getParameter("code");
        if (code == null) {
            response.status400Finish();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecretCipher.decrypt(clientSecret));
        params.put("redirect_uri", redirectUri); // RFC 6749：token 请求的 redirect_uri 必须与授权请求完全一致
        params.put("client_secret", clientSecretCipher.tryDecrypt(clientSecret, "client_secret"));

        Response tokenResponse = httpClient.send(
                httpClient.newRequest(tokenEndpoint).method(HttpMethod.POST).params(params));
        String tokenBodyText = new String(tokenResponse.getBody(), StandardCharsets.UTF_8);
        if (tokenResponse.getStatus() != 200 || json.fromJson(tokenBodyText, Map.class).get("error") != null) {
            // 此授权服务器的错误响应以 HTTP 200 返回（如 invalid_client），仅凭状态码判断会误报为 "failed to get user info"
            response.status500Finish("token exchange failed: " + tokenBodyText);
            return;
        }
        Map<String, Object> tokenBody = json.fromJson(tokenBodyText, Map.class);
        String user = extractUser(tokenBody);
        if (user == null) {
            response.status500Finish("failed to get user info");
            return;
        }

        response.header("Set-Cookie", TOKEN_COOKIE_NAME + "=" + createToken(user)
                        + "; Path=/; HttpOnly; Secure; SameSite=Lax")
        response.header("Set-Cookie", COOKIE_NAME + "=" + createToken(user)
                        + "; Path=/; HttpOnly; SameSite=Lax" + (redirectUri.startsWith("https") ? "; Secure" : "")) // Secure 与否取决于浏览器访问协议，与 redirect_uri 的 scheme 一致：HTTP 部署浏览器会丢弃带 Secure 的 cookie，HTTPS 部署则须防明文传输
                .header("Cache-Control", "no-store")
                .redirect("/"); // 未用 state 暂存回跳路径，故一律重定向到根路径
    }

    /**
     * 身份必须由授权服务器校验：只接受 userinfo endpoint 返回的 sub。
     * 不接受未验签的 id_token——其 payload 可任意伪造，据此登录等同于身份可被直接接管。
     */
    private String extractUser(Map<String, Object> tokenBody) throws Exception {
        String accessToken = (String) tokenBody.get("access_token");
        if (accessToken == null || userinfoEndpoint == null || userinfoEndpoint.isEmpty()) {
            return null;
        }

        Response resp = httpClient.send(httpClient.newRequest(userinfoEndpoint)
                .header("Authorization", "Bearer " + accessToken));
        if (resp.getStatus() != 200) {
            return null;
        }

        Map<String, Object> info = json.fromJson(new String(resp.getBody(), StandardCharsets.UTF_8), Map.class);
        return (String) info.get("sub");
    }

    String getCookie(HttpRequest request, String name) {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null) return null;

        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(name + "=")) {
                return trimmed.substring(name.length() + 1);
        if (accessToken != null && userinfoEndpoint != null && !userinfoEndpoint.isEmpty()) {
            Response resp = httpClient.send(httpClient.newRequest(userinfoEndpoint)
                    .header("Authorization", "Bearer " + accessToken));
            if (resp.getStatus() == 200) {
                Map<String, Object> info = json.fromJson(new String(resp.getBody(), StandardCharsets.UTF_8), Map.class);
                String user = pickUsername(info);
                if (user != null) return user;
            }
        }
        // 回退：从 id_token 的 JWT payload 提取（未验签，仅作提示性解析）
        String idToken = (String) tokenBody.get("id_token");
        if (idToken != null) {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                Map<String, Object> claims = json.fromJson(
                        new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8), Map.class);
                String user = pickUsername(claims);
                if (user != null) return user;
            }
        }
        return null;
    }

    private String pickUsername(Map<String, Object> map) {
        String[] tryUserNames = {usernameField,
                "preferred_username",// OIDC 标准 scope=profile 时属性，服务器决定是否有值
                "name", // OIDC 标准 scope=profile 时属性，服务器决定是否有值
                "sub" // OIDC scope=openid 标准属性，一个id字符串
        };
        for (String field : tryUserNames) {
            String val = str(map.get(field));
            if (val != null) return val;
        }
        return null;
    }

    private String str(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    private String createToken(String user) {
        try {
            return tokenCipher.encrypt(user + "|" + (System.currentTimeMillis() + tokenExpireMillis));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    String verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            int sep = payload.lastIndexOf('|');
            return System.currentTimeMillis() < Long.parseLong(payload.substring(sep + 1))
                    ? payload.substring(0, sep) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
