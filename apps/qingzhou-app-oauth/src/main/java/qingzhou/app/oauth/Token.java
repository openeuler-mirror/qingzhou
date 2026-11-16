package qingzhou.app.oauth;

import java.util.Map;

import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/** 令牌端点：授权码、密码、客户端凭证、刷新令牌四种授予方式。 */
public class Token extends ClientEndpoint {
    private final Oidc oidc;
    private final Throttle throttle;
    private final boolean pkceRequired;

    public Token(Store store, Json json, Oidc oidc, Throttle throttle, boolean pkceRequired) {
        super(store, json);
        this.oidc = oidc;
        this.throttle = throttle;
        this.pkceRequired = pkceRequired;
    }

    @Override
    protected void process(HttpRequest request, Map<String, String> client, HttpResponse response) throws Exception {
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
                result = byAuthCode(request, client);
                break;
            case "password":
                result = byPassword(request, client);
                break;
            case "client_credentials":
                result = issue(request, client, null, false);
                break;
            case "refresh_token":
                result = byRefreshToken(request, client);
                break;
            default:
                result = Reply.error("unsupported_grant_type", "不支持的 grant_type");
        }

        if (result.containsKey("error")) response.status(400);
        Reply.send(response, json, result);
    }

    private Map<String, Object> byAuthCode(HttpRequest request, Map<String, String> client) throws Exception {
        String code = request.getParameter("code");
        if (Security.isEmpty(code)) return Reply.error("invalid_request", "缺少 code");

        Map<String, String> authCode = store.findUsableAuthCode(code, client.get("client_id"));
        if (authCode == null) return Reply.error("invalid_grant", "授权码无效或已过期");

        String redirectUri = request.getParameter("redirect_uri");
        if (!Security.isEmpty(redirectUri) && !redirectUri.equals(authCode.get("redirect_uri"))) {
            return Reply.error("invalid_grant", "redirect_uri 不匹配");
        }

        String challenge = authCode.get("code_challenge");
        if (!Security.isEmpty(challenge)) {
            if (!Security.verifyPkce(request.getParameter("code_verifier"), challenge,
                    authCode.get("code_challenge_method"))) {
                return Reply.error("invalid_grant", "code_verifier 校验失败");
            }
        } else if (Security.isPkceRequired(client, pkceRequired)) {
            return Reply.error("invalid_grant", "该客户端要求 PKCE");
        }

        if (!store.consumeAuthCode(authCode.get("id"))) { // 原子核销，重复提交同一授权码只有一次生效
            return Reply.error("invalid_grant", "授权码无效或已过期");
        }
        String scope = authCode.get("scope");
        if (Security.isEmpty(scope)) scope = store.getDefaultScope();
        Map<String, Object> result = store.issueToken(client.get("client_id"),
                authCode.get("username"), scope, true);
        if (Security.split(scope).contains("openid")) {
            long authTime = parseLong(authCode.get("auth_time"));
            result.put("id_token", oidc.idToken(request, store, client, authCode.get("username"),
                    scope, authCode.get("nonce"), String.valueOf(result.get("access_token")), authTime));
        }
        return result;
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
        return issue(request, client, userName, true);
    }

    private Map<String, Object> byRefreshToken(HttpRequest request, Map<String, String> client) throws Exception {
        String refreshToken = request.getParameter("refresh_token");
        if (Security.isEmpty(refreshToken)) return Reply.error("invalid_request", "缺少 refresh_token");

        Map<String, String> token = store.findValidRefreshToken(refreshToken, client.get("client_id"));
        if (token == null) return Reply.error("invalid_grant", "refresh_token 无效或已过期");

        String scope = token.get("scope");
        String requested = request.getParameter("scope");
        if (!Security.isEmpty(requested)) {
            String invalid = Security.validateScope(requested, scope);
            if (invalid != null) return Reply.error("invalid_scope", invalid);
            scope = requested;
        }

        if (!store.deleteToken(token.get("id"))) { // 刷新即轮换：旧令牌立即失效，并发刷新只有一次生效
            return Reply.error("invalid_grant", "refresh_token 无效或已过期");
        }
        return store.issueToken(client.get("client_id"), token.get("username"), scope, true);
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return Store.now();
        }
    }

    private Map<String, Object> issue(HttpRequest request, Map<String, String> client,
                                      String userName, boolean includeRefresh) throws Exception {
        String scope = Security.scopeOrDefault(request.getParameter("scope"),
                client.get("scope"), store.getDefaultScope());
        String invalid = Security.validateScope(scope, client.get("scope"));
        if (invalid != null) return Reply.error("invalid_scope", invalid);

        return store.issueToken(client.get("client_id"), userName, scope, includeRefresh);
    }
}
