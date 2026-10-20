package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌校验端点：查询访问令牌是否有效。
 */
public class Introspect extends ClientEndpoint {
    public Introspect(Store store, Json json) {
        super(store, json);
    }

    @Override
    protected void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception {
        String accessToken = request.getParameter("token");
        if (Security.isEmpty(accessToken)) {
            Reply.sendError(response, json, "invalid_request", "缺少 token");
            return;
        }

        Map<String, String> token = store.findValidAccessToken(accessToken);
        Map<String, Object> body = new LinkedHashMap<>();
        if (token == null || !client.get("client_id").equals(token.get("client_id"))) { // 只能校验自己的令牌
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
