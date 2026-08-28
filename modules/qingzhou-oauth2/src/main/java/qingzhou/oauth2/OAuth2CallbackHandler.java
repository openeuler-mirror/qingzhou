package qingzhou.oauth2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.TokenService;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

@Component(configurationPid = "qingzhou-oauth2", property = HttpHandler.HANDLE_PATH + "=/callback")
public class OAuth2CallbackHandler implements HttpHandler {
    static final String EXCLUDED_CALLBACK_PATH = "/qingzhou-oauth2/callback";
    static final String COOKIE_NAME = "oauth2_session";

    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;

    @Reference
    private TokenService tokenService;

    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String userinfoEndpoint;

    @Activate
    public void init(Map<String, String> config) {
        clientId = config.get("client_id");
        clientSecret = config.get("client_secret");
        tokenEndpoint = config.get("token_endpoint");
        userinfoEndpoint = config.get("userinfo_endpoint");
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String code = request.getParameter("code");
        if (code == null) {
            response.status400Finish();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);

        Response tokenResponse = httpClient.send(
                httpClient.newRequest(tokenEndpoint).method(HttpMethod.POST).params(params));
        if (tokenResponse.getStatus() != 200) {
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

        response.status(302).header("Location", "/") // 因当前无状态设计，未使用 state 暂存路径等状态，故一律重定向到根路径
                .header("Set-Cookie", COOKIE_NAME + "=" + tokenService.createToken(user)
                        + "; Path=/; HttpOnly; Secure; SameSite=Lax")
                .header("Cache-Control", "no-store")
                .sendFinish("redirecting");
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
}
