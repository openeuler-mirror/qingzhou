package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌注销端点：撤销访问令牌及其关联的刷新令牌。
 */
public class Revoke extends ClientEndpoint {
    public Revoke(Store store, Json json) {
        super(store, json);
    }

    @Override
    protected void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception {
        String token = request.getParameter("token");
        if (Security.isEmpty(token)) {
            token = request.getParameter("access_token");
        }
        if (Security.isEmpty(token)) {
            Reply.sendError(response, json, "invalid_request", "缺少 token");
            return;
        }

        Map<String, String> stored = store.findToken(token);
        if (stored == null || !client.get("client_id").equals(stored.get("client_id"))) { // 只能注销自己的令牌
            Reply.sendError(response, json, "invalid_token", "token 不存在");
            return;
        }

        store.deleteToken(stored.get("id"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        Reply.send(response, json, body);
    }
}
