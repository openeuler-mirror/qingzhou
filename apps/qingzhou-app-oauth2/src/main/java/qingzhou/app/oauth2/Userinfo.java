package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 用户信息端点：凭访问令牌返回资源拥有者信息。
 */
public class Userinfo implements HttpHandler {
    private final Store store;
    private final Json json;

    public Userinfo(Store store, Json json) {
        this.store = store;
        this.json = json;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String accessToken = accessToken(request);
        if (Security.isEmpty(accessToken)) {
            Reply.sendError(response, json, "invalid_request", "缺少 access_token");
            return;
        }

        Map<String, String> token = store.findValidAccessToken(accessToken);
        if (token == null) {
            Reply.sendUnauthorized(response, json, "invalid_token", "access_token 无效或已过期");
            return;
        }

        Map<String, String> user = store.findUser(token.get("username"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", token.get("username"));
        body.put("userName", token.get("username"));
        body.put("nickname", value(user, "nickname"));
        body.put("role", value(user, "rolename"));
        body.put("client_id", token.get("client_id"));
        body.put("scope", token.get("scope"));
        Reply.send(response, json, body);
    }

    private static String accessToken(HttpRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return authorization.substring(7).trim();
        }
        return request.getParameter("access_token");
    }

    private static String value(Map<String, String> row, String key) {
        return row == null ? "" : row.getOrDefault(key, "");
    }
}
