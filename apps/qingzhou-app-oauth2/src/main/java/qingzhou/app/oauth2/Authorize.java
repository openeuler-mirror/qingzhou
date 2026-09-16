package qingzhou.app.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

/**
 * 授权端点：展示登录授权页，签发授权码或隐式令牌。
 */
public class Authorize implements HttpHandler {
    private static final String PAGE = "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>授权 — OAuth 2.0</title>"
            + "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
            + "background:#f5f7fa;color:#333;min-height:100vh;display:flex;align-items:center;justify-content:center}"
            + ".card{background:#fff;border-radius:10px;box-shadow:0 4px 24px rgba(0,0,0,.1);padding:32px;width:420px}"
            + "h2{font-size:20px;margin-bottom:8px;text-align:center}.sub{text-align:center;font-size:13px;color:#999;margin-bottom:24px}"
            + ".form-group{margin-bottom:16px}label{display:block;font-size:13px;color:#666;margin-bottom:4px}"
            + "input{width:100%;padding:10px 12px;border:1px solid #ddd;border-radius:6px;font-size:14px;outline:none}"
            + "input:focus{border-color:#409eff}.info{background:#f0f7ff;border-radius:6px;padding:14px;margin-bottom:20px;font-size:13px}"
            + ".label{color:#999}.value{color:#333;word-break:break-all}.actions{display:flex;gap:10px}"
            + "button{flex:1;padding:11px 0;border:none;border-radius:6px;font-size:14px;cursor:pointer}"
            + ".approve{background:#409eff;color:#fff}.deny{background:#f0f0f0;color:#666}"
            + ".error{background:#fef0f0;color:#e74c3c;padding:10px 14px;border-radius:6px;font-size:13px;margin-bottom:16px}</style></head>"
            + "<body><div class=\"card\"><h2>OAuth 授权</h2><p class=\"sub\">请登录并确认授权</p>{{error}}"
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
            + "<div class=\"form-group\"><label>用户名</label><input type=\"text\" name=\"userName\" placeholder=\"请输入用户名\" required autofocus></div>"
            + "<div class=\"form-group\"><label>密码</label><input type=\"password\" name=\"password\" placeholder=\"请输入密码\" required></div>"
            + "<div class=\"actions\"><button type=\"submit\" name=\"action\" value=\"approve\" class=\"approve\">授权</button>"
            + "<button type=\"submit\" name=\"action\" value=\"deny\" class=\"deny\">拒绝</button></div></form></div></body></html>";

    private final Store store;
    private final Json json;
    private final Throttle throttle;
    private final boolean implicitEnabled;

    public Authorize(Store store, Json json, Throttle throttle, boolean implicitEnabled) {
        this.store = store;
        this.json = json;
        this.throttle = throttle;
        this.implicitEnabled = implicitEnabled;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String responseType = request.getParameter("response_type");
        if (!"code".equals(responseType) && !validImplicit(responseType)) {
            Reply.sendError(response, json, "unsupported_response_type", "response_type 仅支持 code 与 token");
            return;
        }

        Map<String, String> client = store.findClient(request.getParameter("client_id"));
        if (client == null) {
            Reply.sendError(response, json, "invalid_client", "客户端不存在");
            return;
        }

        String invalid = validateRequest(request, client, responseType);
        if (invalid != null) {
            Reply.sendError(response, json, "invalid_request", invalid);
            return;
        }

        if ("POST".equals(request.getMethod())) {
            submit(request, response, client, responseType);
        } else {
            renderPage(request, response, client, responseType, "");
        }
    }

    private void submit(HttpRequest request, HttpResponse response, Map<String, String> client, String responseType) throws Exception {
        String redirectUri = client.get("redirect_uri");
        String state = request.getParameter("state");
        if ("deny".equals(request.getParameter("action"))) {
            Reply.redirect(response, location(redirectUri, "error=access_denied", state));
            return;
        }

        String key = request.getRemoteHost();
        if (throttle.isLocked(key)) {
            renderPage(request, response, client, responseType, "失败次数过多，请稍后再试");
            return;
        }

        String userName = request.getParameter("userName");
        if (!store.verifyPassword(userName, request.getParameter("password"))) {
            throttle.recordFailure(key);
            renderPage(request, response, client, responseType, "用户名或密码错误");
            return;
        }
        throttle.clear(key);

        if ("token".equals(responseType)) {
            implicit(request, response, client, userName);
            return;
        }

        String code = Security.randomToken(16);
        store.saveAuthCode(code, client.get("client_id"), userName, redirectUri, request.getParameter("scope"));
        Reply.redirect(response, location(redirectUri, "code=" + encode(code), state));
    }

    private void implicit(HttpRequest request, HttpResponse response, Map<String, String> client, String userName) throws Exception {
        Map<String, Object> token = store.issueToken(client.get("client_id"), userName, request.getParameter("scope"));
        StringBuilder fragment = new StringBuilder()
                .append("access_token=").append(encode(token.get("access_token")))
                .append("&token_type=").append(encode(token.get("token_type")))
                .append("&expires_in=").append(token.get("expires_in"))
                .append("&scope=").append(encode(token.get("scope")));
        if (!Security.isEmpty(request.getParameter("state"))) {
            fragment.append("&state=").append(encode(request.getParameter("state")));
        }
        Reply.redirect(response, client.get("redirect_uri") + "#" + fragment);
    }

    private void renderPage(HttpRequest request, HttpResponse response, Map<String, String> client, String responseType, String error) {
        response.contentType("text/html; charset=utf-8")
                .header("Cache-Control", "no-store")
                .header("X-Frame-Options", "DENY") // 授权页不允许被嵌套，防止点击劫持
                .sendFinish(page(responseType, client.get("client_name"), client.get("client_id"), client.get("redirect_uri"),
                        request.getParameter("scope"), request.getParameter("state"), error));
    }

    /**
     * @return null 表示校验通过，否则为错误描述
     */
    private String validateRequest(HttpRequest request, Map<String, String> client, String responseType) {
        String grantType = "token".equals(responseType) ? "implicit" : "authorization_code";
        if (!Security.grantAllowed(client, grantType)) return "客户端未登记该 response_type";

        String invalid = Security.validateRedirectUri(request.getParameter("redirect_uri"), client.get("redirect_uri"));
        return invalid != null ? invalid : Security.validateScope(request.getParameter("scope"), client.get("scope"));
    }

    private boolean validImplicit(String responseType) {
        return "token".equals(responseType) && implicitEnabled;
    }

    private static String location(String redirectUri, String params, String state) {
        StringBuilder url = new StringBuilder(redirectUri)
                .append(redirectUri.contains("?") ? '&' : '?')
                .append(params);
        if (!Security.isEmpty(state)) {
            url.append("&state=").append(encode(state));
        }
        return url.toString();
    }

    private static String page(String responseType, String clientName, String clientId, String redirectUri, String scope, String state, String error) {
        String grantType = "token".equals(responseType) ? "Implicit 隐式模式" : "Authorization Code 授权码模式";
        return PAGE
                .replace("{{error}}", error.isEmpty() ? "" : "<div class=\"error\">" + Security.escapeHtml(error) + "</div>")
                .replace("{{grant_type}}", grantType)
                .replace("{{response_type}}", Security.escapeHtml(responseType))
                .replace("{{client_name}}", Security.escapeHtml(clientName))
                .replace("{{client_id}}", Security.escapeHtml(clientId))
                .replace("{{redirect_uri}}", Security.escapeHtml(redirectUri))
                .replace("{{scope}}", Security.escapeHtml(scope))
                .replace("{{state}}", Security.escapeHtml(state));
    }

    private static String encode(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
