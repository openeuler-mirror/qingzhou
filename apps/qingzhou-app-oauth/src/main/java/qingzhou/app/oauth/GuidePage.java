package qingzhou.app.oauth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import qingzhou.http.server.*;

/**
 * 每个接入客户端独立的接入示例页，方便客户端开发方直接照着调用。
 * 页面不含 client_secret，只使用占位符，因此可以安全地分享链接。
 */
public final class GuidePage implements HttpHandler {
    private static final String PREFIX = "/oauth-server";
    private static final String STYLE = ""
            + "body{margin:0;background:#f4f7fb;color:#273142;font:14px/1.65 -apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif}"
            + ".wrap{max-width:1080px;margin:0 auto;padding:28px 20px 48px}"
            + ".hero{background:linear-gradient(120deg,#2d6cdf,#1b9aaa);color:#fff;border-radius:14px;padding:26px 30px;margin-bottom:18px}"
            + ".hero h1{margin:0 0 6px;font-size:24px}.hero p{margin:0;opacity:.9}"
            + ".card{background:#fff;border:1px solid #e3e9f2;border-radius:12px;padding:20px 22px;margin:16px 0;box-shadow:0 3px 12px rgba(30,50,80,.05)}"
            + "h2{font-size:17px;margin:0 0 12px;color:#1d3557}h3{font-size:15px;margin:18px 0 8px}"
            + "p.note{color:#637083;margin:6px 0 12px}"
            + "table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:9px 10px;border-bottom:1px solid #edf1f7;vertical-align:top}"
            + "th{width:180px;color:#637083;font-weight:500}code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace}"
            + "pre{background:#0f172a;color:#dbeafe;border-radius:10px;padding:14px 16px;overflow:auto;margin:8px 0;font-size:12.5px;line-height:1.6}"
            + ".code-head{display:flex;justify-content:flex-end;margin-top:10px}.copy{border:1px solid #cfd9e8;background:#fff;color:#2d6cdf;border-radius:7px;padding:3px 10px;cursor:pointer}"
            + ".badge{display:inline-block;background:#eaf2ff;color:#2d6cdf;border-radius:20px;padding:1px 9px;margin:2px 4px 2px 0;font-size:12px}"
            + ".warn{background:#fff8e8;border-color:#f2dfb6;color:#7a5b18}.muted{color:#7b8798;font-size:12px}"
            + "a{color:#2d6cdf;text-decoration:none}a:hover{text-decoration:underline}"
            + "@media(max-width:640px){th{width:110px}.hero{padding:20px}.wrap{padding:16px}}";
    private static final String SCRIPT = ""
            + "function copyCode(b){const c=b.parentElement.parentElement.querySelector('code');if(!c)return;"
            + "navigator.clipboard.writeText(c.innerText).then(()=>{b.innerText='已复制';setTimeout(()=>b.innerText='复制',1200)}).catch(()=>{});}";

    private final Store store;

    public GuidePage(Store store) {
        this.store = store;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!"GET".equals(request.getMethod())) {
            Reply.methodNotAllowed(response, "GET");
            return;
        }

        String clientId = request.getParameter("client_id");
        Map<String, String> client = Security.isEmpty(clientId) ? null : store.findClient(clientId);
        if (client == null || !Store.isEnabled(client)) {
            notFound(response);
            return;
        }

        String issuer = issuer(request);
        response.status(200)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .header("X-Frame-Options", "DENY")
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'")
                .contentType("text/html; charset=UTF-8")
                .send(render(client, issuer));
    }

    private String issuer(HttpRequest request) {
        String issuer = store.getIssuer();
        if (!Security.isEmpty(issuer)) {
            return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        }
        String host = request.getHeader("Host");
        return request.getScheme() + "://" + (Security.isEmpty(host) ? "localhost" : host) + PREFIX;
    }

    private String render(Map<String, String> client, String issuer) {
        String clientId = value(client, "client_id");
        String clientName = value(client, "client_name");
        String authMethod = value(client, "token_endpoint_auth_method");
        String scope = value(client, "scope");
        String redirects = value(client, "redirect_uri");
        String redirect = first(Security.splitRedirects(redirects), "https://your-app.example/callback");
        Set<String> grants = Security.split(value(client, "grant_types"));
        boolean publicClient = "none".equals(authMethod);
        String secret = publicClient ? "无需密钥（公开客户端）" : "<YOUR_CLIENT_SECRET>";
        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>OAuth 2.0 / OIDC 接入示例 - ").append(text(clientName)).append("</title>")
                .append("<style>").append(STYLE).append("</style></head><body><div class=\"wrap\">")
                .append("<div class=\"hero\"><h1>OAuth 2.0 / OIDC 接入示例</h1><p>")
                .append(text(clientName)).append(" <span class=\"muted\">/ ").append(text(clientId)).append("</span></p></div>");

        appendClientInfo(html, client, authMethod, publicClient, secret);
        appendEndpoints(html, issuer);
        appendClientCredentials(html, issuer, clientId, redirect, scope, secret, authMethod, grants, publicClient);
        html.append(section("用户信息", "使用访问令牌获取资源拥有者信息。",
                "curl \"" + issuer + "/userinfo\" \\\n  -H \"Authorization: Bearer <ACCESS_TOKEN>\"", "GET /userinfo", true));
        html.append(section("令牌内省", "资源服务器校验令牌是否有效，返回 active 等标准字段。",
                "curl -X POST \"" + issuer + "/introspect\" \\\n" + authCurl(clientId, secret, authMethod)
                        + "  -d \"token=<ACCESS_TOKEN>\"", "POST /introspect", true));
        html.append(section("令牌撤销", "撤销访问令牌或刷新令牌；成功后返回 success=true。",
                "curl -X POST \"" + issuer + "/revoke\" \\\n" + authCurl(clientId, secret, authMethod)
                        + "  -d \"token=<ACCESS_TOKEN>\"", "POST /revoke", true));
        html.append(section("服务器元数据", "客户端可读取 RFC 8414 元数据，自动发现端点与能力。",
                "curl \"" + issuer + "/.well-known/oauth-authorization-server\"", "GET /.well-known/oauth-authorization-server", false));
        appendScopes(html, scope);
        html.append("<div class=\"card warn\"><strong>安全提示：</strong>")
                .append(publicClient
                        ? "本页不包含 client_secret；该客户端是公开客户端，无需密钥。"
                        : "本页不包含 client_secret，示例中的 <code>&lt;YOUR_CLIENT_SECRET&gt;</code> 需由管理员另行提供。")
                .append("不要把真实密钥、访问令牌或刷新令牌写入代码仓库或公开日志。")
                .append("</div></div><script>").append(SCRIPT).append("</script></body></html>");
        return html.toString();
    }

    private static void appendClientInfo(StringBuilder html, Map<String, String> client, String authMethod,
                                         boolean publicClient, String secret) {
        html.append("<div class=\"card\"><h2>客户端信息</h2><table>")
                .append(row("客户端 ID", "<code>" + text(value(client, "client_id")) + "</code>"))
                .append(row("客户端名称", text(value(client, "client_name"))))
                .append(row("授予类型", badges(value(client, "grant_types"), GuidePage::grantLabel)))
                .append(row("认证方式", text(authLabel(authMethod))))
                .append(row("授权范围", badges(value(client, "scope"), GuidePage::scopeLabel)))
                .append(row("回调地址", "<code>" + text(first(Security.splitRedirects(value(client, "redirect_uri")), "（未配置）")) + "</code>"))
                .append(row("客户端密钥", publicClient ? "无需密钥（公开客户端）" : text(secret)))
                .append("</table><p class=\"muted\">授权页登录/密码模式使用“接入管理 → 资源拥有者”的账号，"
                        + "不是这里的 client_id / client_secret。</p></div>");
    }

    private static void appendEndpoints(StringBuilder html, String issuer) {
        html.append("<div class=\"card\"><h2>OAuth 2.0 端点</h2><table>")
                .append(endpoint("GET / POST", issuer + "/authorize", "授权端点，支持 code 与 token 响应"))
                .append(endpoint("POST", issuer + "/token", "令牌端点"))
                .append(endpoint("GET / POST", issuer + "/userinfo", "用户信息端点"))
                .append(endpoint("POST", issuer + "/introspect", "令牌内省"))
                .append(endpoint("POST", issuer + "/revoke", "令牌撤销"))
                .append(endpoint("GET", issuer + "/jwks.json", "ID Token 验签公钥（JWKS）"))
                .append(endpoint("GET", issuer + "/.well-known/oauth-authorization-server", "OAuth 服务器元数据"))
                .append(endpoint("GET", issuer + "/.well-known/openid-configuration", "OIDC Discovery 元数据"))
                .append("</table></div>");
    }

    private static void appendClientCredentials(StringBuilder html, String issuer, String clientId,
                                                String redirect, String scope, String secret,
                                                String authMethod, Set<String> grants, boolean publicClient) {
        String credentials = authCurl(clientId, secret, authMethod);
        Set<String> scopeSet = Security.split(scope);
        boolean oidc = scopeSet.contains("openid");
        if (grants.contains("authorization_code")) {
            String code = "# 1. 生成 PKCE verifier 与 S256 challenge（示例值需替换）\n"
                    + "VERIFIER=\"YOUR_CODE_VERIFIER\"\n\n"
                    + "# 2. 浏览器打开授权地址并登录同意；回调会带 code\n"
                    + issuer + "/authorize?response_type=code&client_id=" + urlEncode(clientId)
                    + "&redirect_uri=" + urlEncode(redirect) + "&scope=" + urlEncode(scope)
                    + "&state=YOUR_STATE&code_challenge=YOUR_CODE_CHALLENGE&code_challenge_method=S256"
                    + (oidc ? "&nonce=YOUR_NONCE" : "") + "\n\n"
                    + "# 3. 用 code 换取令牌\n"
                    + "curl -X POST \"" + issuer + "/token\" \\\n"
                    + credentials
                    + "  -d \"grant_type=authorization_code\" \\\n"
                    + "  -d \"code=<AUTHORIZATION_CODE>\" \\\n"
                    + "  -d \"redirect_uri=" + redirect + "\" \\\n"
                    + "  -d \"code_verifier=$VERIFIER\"";
            html.append(section("授权码 + PKCE", "推荐的 Web/移动应用流程；授权码一次性使用，默认 10 分钟过期。"
                            + (oidc ? " 响应中的 id_token 可用 JWKS 验签。" : ""),
                    code, "authorization_code", true));
        }
        if (oidc) {
            String code = "curl \"" + issuer + "/.well-known/openid-configuration\"\n\n"
                    + "curl \"" + issuer + "/jwks.json\"\n\n"
                    + "# 授权码流程的 token 响应会包含 id_token（JWT，RS256）\n"
                    + "# 校验 iss/aud/exp/nonce，并从 jwks_uri 获取公钥验签";
            html.append(section("OIDC Discovery / JWKS", "OpenID Connect 1.0 元数据与 ID Token 验签公钥。",
                    code, "openid", true));
        }
        if (grants.contains("implicit")) {
            String code = issuer + "/authorize?response_type=token&client_id=" + urlEncode(clientId)
                    + "&redirect_uri=" + urlEncode(redirect)
                    + "&scope=" + urlEncode(scope) + "&state=YOUR_STATE"
                    + (oidc ? "&nonce=YOUR_NONCE" : "");
            html.append(section("隐式模式", "仅适用于无法保存密钥的旧客户端；令牌直接从回调 fragment 返回，需服务端开启 implicit_enabled。",
                    code, "implicit", true));
        }
        if (oidc && grants.contains("implicit")) {
            String code = issuer + "/authorize?response_type=id_token&client_id=" + urlEncode(clientId)
                    + "&redirect_uri=" + urlEncode(redirect)
                    + "&scope=" + urlEncode(scope) + "&state=YOUR_STATE&nonce=YOUR_NONCE";
            html.append(section("OIDC 隐式 ID Token", "response_type=id_token 时直接从 fragment 返回 ID Token；nonce 必填。",
                    code, "id_token", true));
        }
        if (grants.contains("client_credentials")) {
            String code = "curl -X POST \"" + issuer + "/token\" \\\n"
                    + credentials
                    + "  -d \"grant_type=client_credentials\" \\\n"
                    + "  -d \"scope=" + scope + "\"";
            html.append(section("客户端凭证", "服务端到服务端调用，不需要资源拥有者；不会签发 refresh_token。",
                    code, "client_credentials", true));
        }
        if (grants.contains("password")) {
            String code = "curl -X POST \"" + issuer + "/token\" \\\n"
                    + credentials
                    + "  -d \"grant_type=password\" \\\n"
                    + "  -d \"userName=<USERNAME>\" \\\n"
                    + "  -d \"password=<PASSWORD>\" \\\n"
                    + "  -d \"scope=" + scope + "\"";
            html.append(section("密码模式", "由客户端直接收集用户名密码；仅用于受信任的第一方客户端。",
                    code, "password", true));
        }
        if (grants.contains("refresh_token")) {
            String code = "curl -X POST \"" + issuer + "/token\" \\\n"
                    + credentials
                    + "  -d \"grant_type=refresh_token\" \\\n"
                    + "  -d \"refresh_token=<REFRESH_TOKEN>\"";
            html.append(section("刷新令牌", "访问令牌过期后使用；服务端会轮换 refresh_token，旧令牌立即失效。",
                    code, "refresh_token", true));
        }
        if (publicClient) {
            html.append("<div class=\"card warn\">当前客户端认证方式为 <code>none</code>，是公开客户端。")
                    .append("请仅在授权码 + PKCE 流程中使用，并确保服务端或客户端开启强制 PKCE。</div>");
        }
        if ("client_secret_basic".equals(authMethod)
                && (grants.contains("client_credentials") || grants.contains("password"))) {
            html.append("<p class=\"muted\">当前客户端使用 client_secret_basic；如改用 client_secret_post，请把 <code>-u ...</code> 改为表单字段 ")
                    .append("<code>client_id</code> 与 <code>client_secret</code>。</p>");
        }
    }

    private static void appendScopes(StringBuilder html, String scope) {
        html.append("<div class=\"card\"><h2>授权范围</h2><table>");
        Set<String> scopes = Security.split(scope);
        for (String item : scopes) {
            html.append("<tr><td><code>").append(text(item)).append("</code></td><td>")
                    .append(text(scopeDescription(item))).append("</td></tr>");
        }
        if (scopes.isEmpty()) html.append("<tr><td colspan=\"2\">未配置</td></tr>");
        html.append("</table><p class=\"muted\">scope 需与服务端 scopes_supported 一致；多个 scope 用空格或逗号分隔。</p></div>");
    }

    private static String section(String title, String description, String code, String badge, boolean copyable) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"card\"><h2>").append(text(title));
        if (!Security.isEmpty(badge)) html.append(" <span class=\"badge\">").append(text(badge)).append("</span>");
        html.append("</h2><p class=\"note\">").append(text(description)).append("</p>");
        if (copyable) {
            html.append("<div class=\"code-head\"><button class=\"copy\" onclick=\"copyCode(this)\">复制</button></div>");
        }
        html.append("<pre><code>").append(text(code)).append("</code></pre></div>");
        return html.toString();
    }

    private static String endpoint(String method, String path, String description) {
        return "<tr><td><span class=\"badge\">" + text(method) + "</span></td><td><code>" + text(path)
                + "</code><br><span class=\"muted\">" + text(description) + "</span></td></tr>";
    }

    private static String row(String label, String value) {
        return "<tr><th>" + text(label) + "</th><td>" + value + "</td></tr>";
    }

    private static String badges(String csv, java.util.function.Function<String, String> label) {
        Set<String> values = Security.split(csv);
        if (values.isEmpty()) return "<span class=\"muted\">未配置</span>";
        StringBuilder html = new StringBuilder();
        for (String value : values)
            html.append("<span class=\"badge\">").append(text(label.apply(value))).append("</span>");
        return html.toString();
    }

    private static String grantLabel(String grant) {
        switch (grant) {
            case "authorization_code":
                return "授权码模式";
            case "password":
                return "密码模式";
            case "client_credentials":
                return "客户端凭证";
            case "refresh_token":
                return "刷新令牌";
            case "implicit":
                return "隐式模式";
            default:
                return grant;
        }
    }

    private static String authLabel(String method) {
        if (Security.isEmpty(method)) return "client_secret_basic（默认）";
        switch (method) {
            case "client_secret_basic":
                return "client_secret_basic（HTTP Basic）";
            case "client_secret_post":
                return "client_secret_post（表单）";
            case "none":
                return "none（公开客户端）";
            default:
                return method;
        }
    }

    private static String scopeLabel(String scope) {
        return scope + "（" + scopeDescription(scope) + "）";
    }

    private static String scopeDescription(String scope) {
        switch (scope) {
            case "read":
                return "只读访问";
            case "write":
                return "读写访问";
            case "openid":
                return "OIDC 身份标识";
            case "profile":
                return "用户资料";
            case "email":
                return "邮箱信息";
            default:
                return "自定义权限范围";
        }
    }

    private static String authCurl(String clientId, String secret, String authMethod) {
        if ("none".equals(authMethod)) {
            return "  -d \"client_id=" + clientId + "\" \\\n";
        }
        if ("client_secret_post".equals(authMethod)) {
            return "  -d \"client_id=" + clientId + "\" \\\n"
                    + "  -d \"client_secret=" + secret + "\" \\\n";
        }
        return "  -u \"" + clientId + ":" + secret + "\" \\\n";
    }

    private static String first(Set<String> values, String fallback) {
        for (String value : values) return value;
        return fallback;
    }

    private static String value(Map<String, String> client, String key) {
        return client.getOrDefault(key, "");
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String text(String value) {
        return Security.escapeHtml(value);
    }

    private static void notFound(HttpResponse response) {
        response.status(404)
                .contentType("text/html; charset=UTF-8")
                .header("Cache-Control", "no-store")
                .send("<!DOCTYPE html><html lang=\"zh-CN\"><meta charset=\"UTF-8\"><title>客户端不存在</title>"
                        + "<body style=\"font-family:sans-serif;padding:40px\"><h1>客户端不存在或已禁用</h1>"
                        + "<p>请检查 client_id，或联系管理员在 OAuth 接入管理中确认客户端状态。</p></body></html>");
    }
}
