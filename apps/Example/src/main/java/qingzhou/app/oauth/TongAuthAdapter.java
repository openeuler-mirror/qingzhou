package qingzhou.app.oauth;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import qingzhou.api.AppContext;
import qingzhou.api.AuthAdapter;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpMethod;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class TongAuthAdapter implements AuthAdapter {
    private final HttpClient httpClient;
    private final Json json;
    private final Logger logger;

    String redirect_uri;
    String client_id;
    String client_secret;
    String authorize_uri;
    String token_uri;
    String user_uri;

    public TongAuthAdapter(AppContext appContext) {
        this.httpClient = appContext.getService(Http.class).buildHttpClient();
        this.json = appContext.getService(Json.class);
        this.logger = appContext.getService(Logger.class);

        Properties p = appContext.getAppProperties();
        String flag = "oauth_";
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith(flag)) {
                String field = key.substring(flag.length());
                String value = p.getProperty(key);

                try {
                    Field declaredField = TongAuthAdapter.class.getDeclaredField(field);
                    declaredField.setAccessible(true);
                    declaredField.set(this, value);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void doAuth(String requestUri, AuthContext context) {
        String receiveCodeUri = "/oauth/code_callback";
        if (receiveCodeUri.equals(requestUri)) {
            String code = context.getParameter("code");
            Map<String, String> tokenInfo;
            try {
                tokenInfo = (Map<String, String>) getToken(code);
            } catch (Exception e) {
                logger.error("failed to get token info", e);
                return;
            }
            String accessToken = tokenInfo.get("access_token");
            String tokenType = tokenInfo.get("token_type");

            Map<String, Object> userInfo;
            try {
                userInfo = (Map<String, Object>) getUser(accessToken, tokenType);
            } catch (Exception e) {
                logger.error("failed to get user info", e);
                return;
            }

            if (Boolean.parseBoolean(String.valueOf(userInfo.get("success")))) {
                Map<String, String> data = (Map<String, String>) userInfo.get("data");
                String user = data.get("userName");
                if (user != null) {
                    String role = data.get("roleName"); // 可能不存在
                    context.setLoginSuccessful(user, role);
                    return;
                }
            }
        }

        String sp = authorize_uri.contains("?") ? "&" : "?";
        String toLoginUrl = authorize_uri
                + sp + "client_id=" + client_id
                + "&response_type=code"
                + "&redirect_uri=" + redirect_uri;

        context.redirect(toLoginUrl);
    }

    private Object getUser(String accessToken, String tokenType) throws Exception {
        String tokenUrl = user_uri + "?client_id=" + client_id;
        HttpResponse httpResponse = httpClient.request(tokenUrl, HttpMethod.GET, (byte[]) null, new HashMap<String, String>() {{
            put("Authorization", tokenType + " " + accessToken);
            put("accept", "application/json");
        }});
        String responseText = new String(httpResponse.getResponseBody(), StandardCharsets.UTF_8);
        return json.fromJson(responseText, Map.class);
    }

    private Object getToken(String code) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", client_id);
        params.put("client_secret", client_secret);
        params.put("code", code);
        HttpResponse httpResponse = httpClient.request(token_uri, HttpMethod.POST, params);
        String responseText = new String(httpResponse.getResponseBody(), StandardCharsets.UTF_8);
        return json.fromJson(responseText, Map.class);
    }
}
