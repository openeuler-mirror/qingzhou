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
    private final Throttle throttle;

    public Token(Store store, Json json, Throttle throttle) {
        this.store = store;
        this.json = json;
        this.throttle = throttle;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"POST".equals(request.getMethod())) { // 凭据不得经 GET 进入 URL 与访问日志
            Reply.methodNotAllowed(response);
            return;
        }

        Map<String, String> client = store.authenticateClient(
                request.getParameter("client_id"), request.getParameter("client_secret"));
        if (client == null) {
            Reply.sendError(response, json, "invalid_client", "客户端验证失败");
            return;
        }

        String grantType = request.getParameter("grant_type");
        if (Security.isEmpty(grantType)) {
            Reply.sendError(response, json, "invalid_request", "缺少 grant_type");
            return;
        }
        if (!Security.grantAllowed(client, grantType)) {
            Reply.sendError(response, json, "unauthorized_client", "客户端未登记该 grant_type");
            return;
        }

        Map<String, Object> result;
        switch (grantType) {
            case "authorization_code":
                result = byAuthCode(request, client.get("client_id"));
                break;
            case "password":
                result = byPassword(request, client);
                break;
            case "client_credentials":
                result = issue(request, client, null);
                break;
            case "refresh_token":
                result = byRefreshToken(request, client.get("client_id"));
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

        if (!store.consumeAuthCode(authCode.get("id"))) { // 原子核销，重复提交同一授权码只有一次生效
            return Reply.error("invalid_grant", "授权码无效或已过期");
        }
        return store.issueToken(clientId, authCode.get("username"), authCode.get("scope"));
    }

    private Map<String, Object> byPassword(HttpRequest request, Map<String, String> client) throws Exception {
        String userName = request.getParameter("userName");
        String key = request.getRemoteHost() + '|' + userName;
        if (throttle.isLocked(key)) return Reply.error("invalid_grant", "失败次数过多，请稍后再试");

        if (!store.verifyPassword(userName, request.getParameter("password"))) {
            throttle.recordFailure(key);
            return Reply.error("invalid_grant", "用户名或密码错误");
        }
        throttle.clear(key);
        return issue(request, client, userName);
    }

    private Map<String, Object> byRefreshToken(HttpRequest request, String clientId) throws Exception {
        Map<String, String> token = store.findValidRefreshToken(request.getParameter("refresh_token"), clientId);
        if (token == null) return Reply.error("invalid_grant", "refresh_token 无效或已过期");

        if (!store.deleteToken(token.get("id"))) { // 刷新即轮换：旧令牌立即失效，并发刷新只有一次生效
            return Reply.error("invalid_grant", "refresh_token 无效或已过期");
        }
        return store.issueToken(clientId, token.get("username"), token.get("scope"));
    }

    private Map<String, Object> issue(HttpRequest request, Map<String, String> client, String userName) throws Exception {
        String scope = request.getParameter("scope");
        String invalid = Security.validateScope(scope, client.get("scope"));
        if (invalid != null) return Reply.error("invalid_scope", invalid);

        return store.issueToken(client.get("client_id"), userName, scope);
    }
}
