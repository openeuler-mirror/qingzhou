package qingzhou.app.oauth;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌内省端点（RFC 7662）：返回令牌当前状态与标准声明。
 */
public class Introspect extends ClientEndpoint {
    public Introspect(Store store, Json json) {
        super(store, json);
    }

    @Override
    protected void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception {
        String tokenValue = request.getParameter("token");
        if (Security.isEmpty(tokenValue)) {
            Reply.sendError(response, json, "invalid_request", "缺少 token");
            return;
        }

        Map<String, String> token = store.findToken(tokenValue);
        Map<String, Object> body = new LinkedHashMap<>();
        if (token == null || !client.get("client_id").equals(token.get("client_id"))) {
            body.put("active", false);
            Reply.send(response, json, body);
            return;
        }

        boolean accessToken = tokenValue.equals(token.get("access_token"));
        String expiresKey = accessToken ? "expires_at" : "refresh_expires_at";
        if (Store.expired(token, expiresKey)) {
            body.put("active", false);
            Reply.send(response, json, body);
            return;
        }

        body.put("active", true);
        put(body, "iss", store.getIssuer());
        body.put("client_id", token.get("client_id"));
        body.put("username", token.get("username"));
        body.put("sub", token.get("username"));
        body.put("aud", token.get("client_id"));
        body.put("scope", token.get("scope"));
        body.put("token_type", accessToken ? token.get("token_type") : "refresh_token");
        body.put("exp", parse(token.get(expiresKey)));
        body.put("iat", parse(token.get("create_time")));
        body.put("nbf", parse(token.get("create_time")));
        Reply.send(response, json, body);
    }

    private static void put(Map<String, Object> body, String key, String value) {
        if (!Security.isEmpty(value)) body.put(key, value);
    }

    private static long parse(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
