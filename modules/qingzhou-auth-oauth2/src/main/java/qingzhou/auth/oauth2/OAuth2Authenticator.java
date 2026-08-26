package qingzhou.auth.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.AuthLoginService;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.*;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

/**
 * OAuth2 Authorization Code 认证器（浏览器场景）。
 * 无 session cookie 时返回 challenge（302 重定向至授权服务器）；回调端点交换 code 后签发签名 token 作为 session cookie。
 * 会话签发/校验复用 AuthLoginService（TokenAuthenticator），与 Bearer Token 同一密钥体系。
 * 与 TokenAuthenticator 并存时：无凭据浏览器 -> challenge（302 登录），无效 token -> reject（401），有效 token -> pass。
 * 注意：id_token 仅作提示性解析，生产环境须校验 JWT 签名或配置 userinfo_endpoint 获取身份。
 */
@Component(immediate = true, configurationPid = "qingzhou-auth-oauth2",
        service = {HttpAuthenticator.class, HttpHandler.class},
        property = HttpHandler.HANDLE_PATH + "=/callback")
public class OAuth2Authenticator implements HttpAuthenticator, HttpHandler {
    private static final String COOKIE_NAME = "oauth2_session";
    private static final String[] EXCLUDED_PATHS = {"/auth-oauth2/callback"};
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final long STATE_TTL_MILLIS = 10 * 60_000;

    @Reference
    private AuthLoginService authLoginService;
    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;

    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>(); // state -> 原路径，防登录 CSRF

    @Activate
    public void start(Map<String, String> config) {
        authorizationEndpoint = config.get("authorization_endpoint");
        tokenEndpoint = config.get("token_endpoint");
        userinfoEndpoint = config.get("userinfo_endpoint");
        clientId = config.get("client_id");
        clientSecret = config.get("client_secret");
        redirectUri = config.get("redirect_uri");
        scope = config.get("scope");
    }

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String cookie = getCookie(request.getHeader("Cookie"), COOKIE_NAME);
        if (cookie == null) {
            String state = URL_ENCODER.encodeToString(randomBytes(16));
            pendingStates.put(state, new PendingState(request.getPath()));
            cleanStates();
            return AuthResult.challenge(buildAuthorizationUrl(state));
        }
        String user = authLoginService.verifyToken(cookie);
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid session");
    }

    @Override
    public String[] excludedPaths() {
        return EXCLUDED_PATHS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String code = request.getParameter("code");
        PendingState pending = pendingStates.remove(request.getParameter("state")); // 验证 state 防登录 CSRF
        if (code == null || pending == null) {
            response.status400Finish();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);

        Response tokenResponse = httpClient.send(
                httpClient.newRequest(tokenEndpoint).method(HttpMethod.POST).params(params));
        if (tokenResponse.getStatus() != 200) {
            logger.error("oauth2 token exchange failed: " + tokenResponse.getStatus());
            response.status(500).sendFinish("token exchange failed");
            return;
        }

        Map<String, Object> tokenBody = json.fromJson(
                new String(tokenResponse.getBody(), StandardCharsets.UTF_8), Map.class);
        String user = extractUser(tokenBody);
        if (user == null) {
            response.status(500).sendFinish("failed to get user info");
            return;
        }

        response.status(302).header("Location", pending.path)
                .header("Set-Cookie", COOKIE_NAME + "=" + authLoginService.createToken(user)
                        + "; Path=/; HttpOnly; Secure; SameSite=Lax")
                .header("Cache-Control", "no-store")
                .sendFinish("redirecting");
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

    @SuppressWarnings("unchecked")
    private String extractUser(Map<String, Object> tokenBody) throws Exception {
        // 优先走 userinfo endpoint（由授权服务器校验 access_token，身份可信）
        String accessToken = (String) tokenBody.get("access_token");
        if (accessToken != null && userinfoEndpoint != null) {
            Response resp = httpClient.send(httpClient.newRequest(userinfoEndpoint)
                    .header("Authorization", "Bearer " + accessToken));
            if (resp.getStatus() == 200) {
                Map<String, Object> info = json.fromJson(new String(resp.getBody(), StandardCharsets.UTF_8), Map.class);
                String sub = (String) info.get("sub");
                if (sub != null) return sub;
            }
        }
        // 回退：从 id_token 的 JWT payload 提取（未验签，仅作提示性解析）
        String idToken = (String) tokenBody.get("id_token");
        if (idToken != null) {
            String[] parts = idToken.split("\\.");
            if (parts.length >= 2) {
                Map<String, Object> claims = json.fromJson(
                        new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8), Map.class);
                return (String) claims.get("sub");
            }
        }
        return null;
    }

    private void cleanStates() {
        if (pendingStates.size() <= 1000) return;
        long now = System.currentTimeMillis();
        pendingStates.entrySet().removeIf(entry -> now - entry.getValue().createdAt > STATE_TTL_MILLIS);
    }

    private static String getCookie(String cookieHeader, String name) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(name + "=")) return trimmed.substring(name.length() + 1);
        }
        return null;
    }

    private static String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static final class PendingState {
        final long createdAt = System.currentTimeMillis();
        final String path;

        PendingState(String path) {
            this.path = path;
        }
    }
}
