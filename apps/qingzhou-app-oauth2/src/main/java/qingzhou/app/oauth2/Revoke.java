package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌撤销端点（RFC 7009）：撤销访问令牌及其关联刷新令牌。
 *
 * 未知令牌或不属于当前客户端的令牌也返回成功，避免把端点变成令牌探测工具。
 */
public class Revoke extends ClientEndpoint {
    public Revoke(Store store, Json json, boolean allowAnonymous) {
        super(store, json, allowAnonymous);
    }

    @Override
    protected void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception {
        String token = request.getParameter("token");
        if (Security.isEmpty(token)) token = request.getParameter("access_token");
        if (Security.isEmpty(token)) {
            Reply.sendError(response, json, "invalid_request", "缺少 token");
            return;
        }

        Map<String, String> stored = store.findToken(token);
        if (stored != null) {
            // client 为空表示 token-only 匿名注销；有 client 时只允许撤销自己的 token。
            // 不属于当前客户端的令牌仍返回 success=true，避免把端点变成令牌探测工具。
            if (client == null || client.get("client_id").equals(stored.get("client_id"))) {
                store.deleteToken(stored.get("id"));
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        Reply.send(response, json, body);
    }
}
