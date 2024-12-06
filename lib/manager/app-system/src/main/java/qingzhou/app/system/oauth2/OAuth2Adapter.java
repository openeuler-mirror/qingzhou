package qingzhou.app.system.oauth2;

import qingzhou.api.AuthAdapter;
import qingzhou.app.system.Main;
import qingzhou.config.OAuth2;
import qingzhou.http.Http;
import qingzhou.http.HttpClient;
import qingzhou.http.HttpResponse;
import qingzhou.json.Json;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OAuth2Adapter implements AuthAdapter {
    private final HttpClient httpClient = Main.getService(Http.class).buildHttpClient();
    private final Json json = Main.getService(Json.class);
    private final OAuth2 config;

    public OAuth2Adapter(OAuth2 config) {
        this.config = config;
    }

    @Override
    public String getLoginUri() {
        return "oauth2/code_callback";
    }

    @Override
    public boolean login(AuthContext context) throws Exception {
        String code = context.getParameter("code");
        String accessToken = getToken(code);

        String user = getUser(accessToken);
        if (user != null) {
            context.setUser(user);
            return true;
        }
        return false;
    }

    private String getUser(String token) throws Exception {
        String tokenUrl = config.getUser_uri() + "?client_id=" + config.getClient_id();
        HttpResponse httpResponse = httpClient.get(tokenUrl, new HashMap<String, String>() {{
            put("Authorization", "Bearer " + token);
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

    private String getToken(String code) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", config.getClient_id());
        params.put("client_secret", config.getClient_secret());
        params.put("code", code);
        HttpResponse httpResponse = httpClient.post(config.getToken_uri(), params);
        String responseText = new String(httpResponse.getResponseBody(), StandardCharsets.UTF_8);
        Map<String, String> result = json.fromJson(responseText, Map.class);
        return result.get("access_token");
    }
}
