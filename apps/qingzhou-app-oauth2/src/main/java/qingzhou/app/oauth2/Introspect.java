package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌校验端点：查询访问令牌是否有效。
 */
public class Introspect implements HttpHandler {
    private final Store store;
    private final Json json;

    public Introspect(Store store, Json json) {
        this.store = store;
        this.json = json;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String accessToken = request.getParameter("token");
        if (Security.isEmpty(accessToken)) {
            Reply.sendError(response, json, "invalid_request", "缺少 token");
            return;
        }

        Map<String, String> token = store.findValidAccessToken(accessToken);
        Map<String, Object> body = new LinkedHashMap<>();
        if (token == null) {
            body.put("active", false);
        } else {
            body.put("active", true);
            body.put("client_id", token.get("client_id"));
            body.put("userName", token.get("username"));
            body.put("scope", token.get("scope"));
            body.put("token_type", token.get("token_type"));
            body.put("exp", Long.parseLong(token.get("expires_at")));
        }
        Reply.send(response, json, body);
    }
}
