package qingzhou.app.system.oauth;

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

public class TongAuthAdapter implements AuthAdapter {
    private final HttpClient httpClient;
    private final Json json;

    String redirect_uri;
    String client_id;
    String client_secret;
    String authorize_uri;
    String token_uri;
    String user_uri;

    public TongAuthAdapter(AppContext appContext) {
        this.httpClient = appContext.getService(Http.class).buildHttpClient();
        this.json = appContext.getService(Json.class);

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
    public void doAuth(String requestUri, AuthContext context) throws Exception {
        String receiveCodeUri = "/oauth/code_callback";
        if (receiveCodeUri.equals(requestUri)) {
            String code = context.getParameter("code");
            Map<String, String> tokenInfo = getToken(code);
            String user = getUser(tokenInfo);
            if (user != null) {
                context.setLoginSuccessful(user);
                return;
            }
        }

        String sp = authorize_uri.contains("?") ? "&" : "?";
        String toLoginUrl = authorize_uri
                + sp + "client_id=" + client_id
                + "&response_type=code"
                + "&redirect_uri=" + redirect_uri;

        context.redirect(toLoginUrl);
    }

    private String getUser(Map<String, String> tokenInfo) throws Exception {
        String tokenUrl = user_uri + "?client_id=" + client_id;

        String accessToken = tokenInfo.get("access_token");
        String tokenType = tokenInfo.get("token_type");
        HttpResponse httpResponse = httpClient.request(tokenUrl, HttpMethod.GET, (byte[]) null, new HashMap<String, String>() {{
            put("Authorization", tokenType + " " + accessToken);
            put("accept", "application/json");
        }});
        String responseText = new String(httpResponse.getResponseBody(), StandardCharsets.UTF_8);
        Map<String, Object> result = json.fromJson(responseText, Map.class);

        if (Boolean.parseBoolean(String.valueOf(result.get("success")))) {
            Map<String, String> data = (Map<String, String>) result.get("data");
            return data.get("userName");
        }
        return null;
    }

    private Map<String, String> getToken(String code) throws Exception {
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
