package qingzhou.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        property = {HttpHandler.HANDLE_PATH + "=/", HttpHandler.HANDLE_NO_AUTH + "=true"})
public class OAuth2Handler implements HttpHandler {
    private static final String AUTHORIZE_PATH = "/oauth2/authorize";
    private static final String CALLBACK_PATH = "/oauth2/callback";
    static final String COOKIE_NAME = "oauth2_session";

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

    private long tokenExpireMillis;
    private static Cipher tokenCipher;
    private static Cipher clientSecretCipher;

    @Activate
    public void init(Map<String, String> config) {
        authorizationEndpoint = config.get("authorize_endpoint");
        clientId = config.get("client_id");
        clientSecret = config.get("client_secret"); //安全考虑：此处不要解密
        tokenEndpoint = config.get("token_endpoint");
        userinfoEndpoint = config.get("userinfo_endpoint");
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
            String authorizationUrl = buildAuthorizationUrl();
            response.redirect(authorizationUrl);
        } else if (path.equals(CALLBACK_PATH)) {
            callback(request, response);
        } else {
            response.status400Finish();
        }
    }

    private void callback(HttpRequest request, HttpResponse response) throws Exception {
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

        Response tokenResponse = httpClient.send(
                httpClient.newRequest(tokenEndpoint).method(HttpMethod.POST).params(params));
        if (tokenResponse.getStatus() != 200) {
            response.status500Finish("token exchange failed");
            return;
        }

        Map<String, Object> tokenBody = json.fromJson(
                new String(tokenResponse.getBody(), StandardCharsets.UTF_8), Map.class);
        String user = extractUser(tokenBody);
        if (user == null) {
            response.status500Finish("failed to get user info");
            return;
        }

        response.header("Set-Cookie", COOKIE_NAME + "=" + createToken(user)
                        + "; Path=/; HttpOnly; Secure; SameSite=Lax")
                .header("Cache-Control", "no-store")
                .redirect("/"); // 因当前无状态设计，未使用 state 暂存路径等状态，故一律重定向到根路径
    }

    private String buildAuthorizationUrl() {
        String state = "0"; // 无状态设计，不可在单机上随机生成
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

    private String extractUser(Map<String, Object> tokenBody) throws Exception {
        // 优先走 userinfo endpoint（由授权服务器校验 access_token，身份可信）
        String accessToken = (String) tokenBody.get("access_token");
        if (accessToken != null && userinfoEndpoint != null && !userinfoEndpoint.isEmpty()) {
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

    private String createToken(String user) {
        try {
            return tokenCipher.encrypt(user + "|" + (System.currentTimeMillis() + tokenExpireMillis));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String verifyToken(String token) {
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
