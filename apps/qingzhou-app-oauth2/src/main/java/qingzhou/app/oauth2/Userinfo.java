package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/** 用户信息端点：凭访问令牌返回资源拥有者信息。 */
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
            Reply.sendUnauthorized(response, json, "invalid_token", "缺少 access_token");
            return;
        }

        Map<String, String> token = store.findValidAccessToken(accessToken);
        if (token == null || Security.isEmpty(token.get("username"))) {
            Reply.sendUnauthorized(response, json, "invalid_token", "access_token 无效、已过期或未绑定用户");
            return;
        }

        Map<String, String> user = store.findUser(token.get("username"));
        if (user == null || !Store.isEnabled(user)) {
            Reply.sendUnauthorized(response, json, "invalid_token", "资源拥有者不存在或已禁用");
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", token.get("username"));
        body.put("preferred_username", token.get("username"));
        body.put("userName", token.get("username"));
        body.put("name", value(user, "nickname"));
        body.put("nickname", value(user, "nickname"));
        Set<String> scopes = Security.split(token.get("scope"));
        if (scopes.contains("email") && !Security.isEmpty(value(user, "email"))) {
            body.put("email", value(user, "email"));
        }
        body.put("role", value(user, "rolename"));
        body.put("roles", value(user, "rolename"));
        body.put("client_id", token.get("client_id"));
        body.put("scope", token.get("scope"));
        if (!Security.isEmpty(store.getIssuer())) body.put("iss", store.getIssuer());
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
