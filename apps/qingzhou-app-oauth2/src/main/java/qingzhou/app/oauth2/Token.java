package qingzhou.app.oauth2;

import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 令牌端点：授权码、密码、客户端凭证、刷新令牌四种授予方式。
 */
public class Token implements HttpHandler {
    private final Store store;
    private final Json json;

    public Token(Store store, Json json) {
        this.store = store;
        this.json = json;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String clientId = request.getParameter("client_id");
        Map<String, String> client = store.findClient(clientId);
        if (client == null || !client.get("client_secret").equals(request.getParameter("client_secret"))) {
            Reply.sendError(response, json, "invalid_client", "客户端验证失败");
            return;
        }

        Map<String, Object> result;
        switch (String.valueOf(request.getParameter("grant_type"))) {
            case "authorization_code":
                result = byAuthCode(request, clientId);
                break;
            case "password":
                result = byPassword(request, clientId);
                break;
            case "client_credentials":
                result = store.issueToken(clientId, null, request.getParameter("scope"));
                break;
            case "refresh_token":
                result = byRefreshToken(request, clientId);
                break;
            default:
                result = Reply.error("unsupported_grant_type", "不支持的 grant_type");
        }

        if (result.containsKey("error")) {
            response.status(400); // 协议错误统一用 400，成功仍是 200
        }
        Reply.send(response, json, result);
    }

    private Map<String, Object> byAuthCode(HttpRequest request, String clientId) throws Exception {
        Map<String, String> authCode = store.findUsableAuthCode(request.getParameter("code"), clientId);
        if (authCode == null) return Reply.error("invalid_grant", "授权码无效或已过期");

        String redirectUri = request.getParameter("redirect_uri");
        if (!Security.isEmpty(redirectUri) && !redirectUri.equals(authCode.get("redirect_uri"))) {
            return Reply.error("invalid_grant", "redirect_uri 不匹配");
        }

        store.consumeAuthCode(authCode.get("id"));
        return store.issueToken(clientId, authCode.get("username"), authCode.get("scope"));
    }

    private Map<String, Object> byPassword(HttpRequest request, String clientId) throws Exception {
        String userName = request.getParameter("userName");
        if (!store.verifyPassword(userName, request.getParameter("password"))) {
            return Reply.error("invalid_grant", "用户名或密码错误");
        }
        return store.issueToken(clientId, userName, request.getParameter("scope"));
    }

    private Map<String, Object> byRefreshToken(HttpRequest request, String clientId) throws Exception {
        Map<String, String> token = store.findValidRefreshToken(request.getParameter("refresh_token"), clientId);
        if (token == null) return Reply.error("invalid_grant", "refresh_token 无效或已过期");

        store.deleteToken(token.get("id")); // 刷新即轮换，旧令牌立即失效
        return store.issueToken(clientId, token.get("username"), token.get("scope"));
    }
}
