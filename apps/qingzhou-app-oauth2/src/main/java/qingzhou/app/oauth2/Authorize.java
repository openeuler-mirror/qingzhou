package qingzhou.app.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 授权端点：登录授权页，签发授权码或隐式令牌，支持 PKCE 与 OIDC ID Token。
 */
public class Authorize implements HttpHandler {
    private static final String NO_USERS = "尚未创建资源拥有者账号，请先在“接入管理 → 资源拥有者”中新增用户，"
            + "再用该用户的用户名和密码登录";

    private static final String PAGE = "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>授权 - OAuth 2.0 / OIDC</title>"
            + "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
            + "background:#f5f7fa;color:#333;min-height:100vh;display:flex;align-items:center;justify-content:center}"
            + ".card{background:#fff;border-radius:10px;box-shadow:0 4px 24px rgba(0,0,0,.1);padding:32px;width:430px}"
            + "h2{font-size:20px;margin-bottom:8px;text-align:center}.sub{text-align:center;font-size:13px;color:#999;margin-bottom:24px}"
            + ".form-group{margin-bottom:16px}label{display:block;font-size:13px;color:#666;margin-bottom:4px}"
            + "input{width:100%;padding:10px 12px;border:1px solid #ddd;border-radius:6px;font-size:14px;outline:none}"
            + "input:focus{border-color:#409eff}.info{background:#f0f7ff;border-radius:6px;padding:14px;margin-bottom:20px;font-size:13px}"
            + ".label{color:#999}.value{color:#333;word-break:break-all}.actions{display:flex;gap:10px}"
            + "button{flex:1;padding:11px 0;border:none;border-radius:6px;font-size:14px;cursor:pointer}"
            + ".approve{background:#409eff;color:#fff}.deny{background:#f0f0f0;color:#666}"
            + ".error{background:#fef0f0;color:#e74c3c;padding:10px 14px;border-radius:6px;font-size:13px;margin-bottom:16px}"
            + ".hint{background:#fff8e8;color:#7a5b18;padding:10px 12px;border-radius:6px;font-size:12px;margin-bottom:18px}</style></head>"
            + "<body><div class=\"card\"><h2>OAuth / OIDC 授权</h2><p class=\"sub\">请登录并确认授权</p>"
            + "<p class=\"hint\">请使用“资源拥有者”账号登录；接入客户端的 client_id / client_secret 不能用于此处。</p>{{error}}"
            + "<div class=\"info\"><p><span class=\"label\">客户端：</span><span class=\"value\">{{client_name}}</span></p>"
            + "<p><span class=\"label\">客户端 ID：</span><span class=\"value\">{{client_id}}</span></p>"
            + "<p><span class=\"label\">授权类型：</span><span class=\"value\">{{grant_type}}</span></p>"
            + "<p><span class=\"label\">授权范围：</span><span class=\"value\">{{scope}}</span></p>"
            + "<p><span class=\"label\">回调地址：</span><span class=\"value\">{{redirect_uri}}</span></p></div>"
            + "<form method=\"post\" action=\"\">"
            + "<input type=\"hidden\" name=\"response_type\" value=\"{{response_type}}\">"
            + "<input type=\"hidden\" name=\"client_id\" value=\"{{client_id}}\">"
            + "<input type=\"hidden\" name=\"redirect_uri\" value=\"{{redirect_uri}}\">"
            + "<input type=\"hidden\" name=\"scope\" value=\"{{scope}}\">"
            + "<input type=\"hidden\" name=\"state\" value=\"{{state}}\">"
            + "<input type=\"hidden\" name=\"nonce\" value=\"{{nonce}}\">"
            + "<input type=\"hidden\" name=\"code_challenge\" value=\"{{code_challenge}}\">"
            + "<input type=\"hidden\" name=\"code_challenge_method\" value=\"{{code_challenge_method}}\">"
            + "<div class=\"form-group\"><label>资源拥有者用户名</label><input type=\"text\" name=\"userName\" placeholder=\"请输入资源拥有者用户名\" required autofocus></div>"
            + "<div class=\"form-group\"><label>密码</label><input type=\"password\" name=\"password\" placeholder=\"请输入资源拥有者密码\" required></div>"
            + "<div class=\"actions\"><button type=\"submit\" name=\"action\" value=\"approve\" class=\"approve\">授权</button>"
            + "<button type=\"submit\" name=\"action\" value=\"deny\" class=\"deny\">拒绝</button></div></form></div></body></html>";

    private final Store store;
    private final Json json;
    private final Oidc oidc;
    private final Throttle throttle;
    private final boolean implicitEnabled;
    private final boolean pkceRequired;

    public Authorize(Store store, Json json, Oidc oidc, Throttle throttle,
                     boolean implicitEnabled, boolean pkceRequired) {
        this.store = store;
        this.json = json;
        this.oidc = oidc;
        this.throttle = throttle;
        this.implicitEnabled = implicitEnabled;
        this.pkceRequired = pkceRequired;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String responseType = request.getParameter("response_type");
        String clientId = request.getParameter("client_id");
        Map<String, String> client = store.findClient(clientId);
        if (client == null || !Store.isEnabled(client)) {
            Reply.sendError(response, json, "invalid_client", "客户端不存在或已禁用");
            return;
        }

        Security.Redirect redirect = Security.resolveRedirect(request.getParameter("redirect_uri"), client.get("redirect_uri"));
        if (redirect.error != null) {
            Reply.sendError(response, json, "invalid_request", redirect.error);
            return;
        }

        String requestedScope = request.getParameter("scope");
        String scope = Security.scopeOrDefault(requestedScope, client.get("scope"), store.getDefaultScope());
        String state = request.getParameter("state");
        String nonce = request.getParameter("nonce");
        String challenge = request.getParameter("code_challenge");
        String challengeMethod = request.getParameter("code_challenge_method");
        String error = validateRequest(client, responseType, scope, nonce, challenge, challengeMethod);
        if (error != null) {
            redirectError(response, redirect.uri, errorCode(error), error, state);
            return;
        }

        if ("POST".equals(request.getMethod())) {
            submit(request, response, client, responseType, redirect.uri, scope, state, nonce, challenge, challengeMethod);
        } else if ("GET".equals(request.getMethod())) {
            renderPage(response, client, responseType, redirect.uri, scope, state, nonce,
                    challenge, challengeMethod, store.hasUsers() ? "" : NO_USERS);
        } else {
            Reply.methodNotAllowed(response, "GET, POST");
        }
    }

    private void submit(HttpRequest request, HttpResponse response, Map<String, String> client,
                        String responseType, String redirectUri, String scope, String state, String nonce,
                        String challenge, String challengeMethod) throws Exception {
        if ("deny".equals(request.getParameter("action"))) {
            redirectError(response, redirectUri, "access_denied", "资源拥有者拒绝了授权", state);
            return;
        }

        String key = request.getRemoteHost() + '|' + client.get("client_id");
        if (throttle.isLocked(key)) {
            renderPage(response, client, responseType, redirectUri, scope, state, nonce,
                    challenge, challengeMethod, "失败次数过多，请稍后再试");
            return;
        }

        String userName = request.getParameter("userName");
        if (!store.hasUsers()) {
            renderPage(response, client, responseType, redirectUri, scope, state, nonce,
                    challenge, challengeMethod, NO_USERS);
            return;
        }
        if (!store.verifyPassword(userName, request.getParameter("password"))) {
            throttle.recordFailure(key);
            renderPage(response, client, responseType, redirectUri, scope, state, nonce,
                    challenge, challengeMethod, "用户名或密码错误；请使用资源拥有者账号，而不是接入客户端的 client_id / client_secret");
            return;
        }
        throttle.clear(key);

        if (!hasType(responseType, "code")) {
            implicit(request, response, client, userName, responseType, redirectUri, scope, state, nonce);
            return;
        }

        long now = Store.now();
        String code = Security.randomToken(32);
        Map<String, String> authCode = new LinkedHashMap<>();
        authCode.put("id", java.util.UUID.randomUUID().toString());
        authCode.put("code", code);
        authCode.put("client_id", client.get("client_id"));
        authCode.put("username", userName);
        authCode.put("redirect_uri", redirectUri);
        authCode.put("scope", scope);
        authCode.put("code_challenge", challenge);
        authCode.put("code_challenge_method", challengeMethod);
        authCode.put("nonce", nonce);
        authCode.put("auth_time", String.valueOf(now));
        authCode.put("expires_at", String.valueOf(now + store.authCodeTtl));
        authCode.put("used", "0");
        authCode.put("create_time", String.valueOf(now));
        store.saveAuthCode(authCode);
        redirect(response, location(redirectUri, "code=" + encode(code), state));
    }

    private void implicit(HttpRequest request, HttpResponse response, Map<String, String> client,
                          String userName, String responseType, String redirectUri, String scope,
                          String state, String nonce) throws Exception {
        boolean withAccess = hasType(responseType, "token");
        boolean withIdToken = hasType(responseType, "id_token");
        Map<String, Object> token = withAccess
                ? store.issueToken(client.get("client_id"), userName, scope, false) : null;
        String accessToken = token == null ? null : String.valueOf(token.get("access_token"));
        String idToken = withIdToken
                ? oidc.idToken(request, store, client, userName, scope, nonce, accessToken, Store.now()) : null;

        StringBuilder fragment = new StringBuilder();
        if (withAccess) {
            fragment.append("access_token=").append(encode(accessToken))
                    .append("&token_type=").append(encode(token.get("token_type")))
                    .append("&expires_in=").append(token.get("expires_in"))
                    .append("&scope=").append(encode(token.get("scope")));
        }
        if (withIdToken) {
            if (fragment.length() > 0) fragment.append('&');
            fragment.append("id_token=").append(encode(idToken));
        }
        if (!Security.isEmpty(state)) {
            if (fragment.length() > 0) fragment.append('&');
            fragment.append("state=").append(encode(state));
        }
        redirect(response, redirectUri + "#" + fragment);
    }

    private String validateRequest(Map<String, String> client, String responseType, String scope,
                                   String nonce, String challenge, String challengeMethod) {
        Set<String> types = Security.split(responseType);
        boolean code = types.contains("code");
        boolean accessToken = types.contains("token");
        boolean idToken = types.contains("id_token");
        boolean valid = (types.size() == 1 && (code || accessToken || idToken))
                || (types.size() == 2 && accessToken && idToken);
        if (!valid) return "response_type 仅支持 code、token、id_token、id_token token";

        if (code && !Security.grantAllowed(client, "authorization_code")) {
            return "客户端未登记 authorization_code";
        }
        if ((accessToken || idToken) && (!implicitEnabled || !Security.grantAllowed(client, "implicit"))) {
            return "客户端未登记 implicit 或服务端未启用";
        }
        if (idToken) {
            if (!Security.split(scope).contains("openid")) return "scope 必须包含 openid";
            if (Security.isEmpty(nonce)) return "OIDC 隐式流程必须提供 nonce";
        }

        String invalid = Security.validateScope(scope, client.get("scope"));
        if (invalid != null) return invalid;
        return Security.validatePkce(challenge, challengeMethod,
                Security.isPkceRequired(client, pkceRequired));
    }

    private static boolean hasType(String responseType, String type) {
        return Security.split(responseType).contains(type);
    }

    private static String errorCode(String error) {
        if (error == null) return "invalid_request";
        if (error.startsWith("scope ")) return "invalid_scope";
        if (error.contains("response_type")) return "unsupported_response_type";
        if (error.contains("未登记")) return "unauthorized_client";
        return "invalid_request";
    }

    private void renderPage(HttpResponse response, Map<String, String> client, String responseType,
                            String redirectUri, String scope, String state, String nonce,
                            String challenge, String challengeMethod, String error) {
        response.contentType("text/html; charset=utf-8")
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .header("X-Frame-Options", "DENY") // 授权页不允许被嵌套，防止点击劫持
                .send(page(responseType, client.get("client_name"), client.get("client_id"), redirectUri,
                        scope, state, nonce, challenge, challengeMethod, error));
    }

    private static String page(String responseType, String clientName, String clientId, String redirectUri,
                               String scope, String state, String nonce, String challenge,
                               String challengeMethod, String error) {
        String grantType = grantLabel(responseType);
        return PAGE
                .replace("{{error}}", Security.isEmpty(error) ? ""
                        : "<div class=\"error\">" + Security.escapeHtml(error) + "</div>")
                .replace("{{grant_type}}", grantType)
                .replace("{{response_type}}", Security.escapeHtml(responseType))
                .replace("{{client_name}}", Security.escapeHtml(clientName))
                .replace("{{client_id}}", Security.escapeHtml(clientId))
                .replace("{{redirect_uri}}", Security.escapeHtml(redirectUri))
                .replace("{{scope}}", Security.escapeHtml(scope))
                .replace("{{state}}", Security.escapeHtml(state))
                .replace("{{nonce}}", Security.escapeHtml(nonce))
                .replace("{{code_challenge}}", Security.escapeHtml(challenge))
                .replace("{{code_challenge_method}}", Security.escapeHtml(challengeMethod));
    }

    private static String grantLabel(String responseType) {
        Set<String> types = Security.split(responseType);
        if (types.contains("code")) return "Authorization Code 授权码模式";
        if (types.contains("id_token") && types.contains("token")) return "OIDC Implicit（id_token + token）";
        if (types.contains("id_token")) return "OIDC Implicit（id_token）";
        return "Implicit 隐式模式";
    }

    private static void redirectError(HttpResponse response, String redirectUri, String code,
                                      String description, String state) {
        String location = location(redirectUri, "error=" + encode(code) + "&error_description=" + encode(description), state);
        redirect(response, location);
    }

    private static void redirect(HttpResponse response, String location) {
        Reply.redirect(response, location);
    }

    private static String location(String redirectUri, String params, String state) {
        StringBuilder url = new StringBuilder(redirectUri)
                .append(redirectUri.contains("?") ? '&' : '?')
                .append(params);
        if (!Security.isEmpty(state)) url.append("&state=").append(encode(state));
        return url.toString();
    }

    private static String encode(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
