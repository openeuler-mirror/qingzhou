package qingzhou.auth.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.auth.TokenService;
import qingzhou.common.Configs;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.client.HttpClient;
import qingzhou.http.client.HttpMethod;
import qingzhou.http.client.Response;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + OAuth2Handler.ROOT_PATH,
                HttpHandler.HANDLE_PATH_ABSOLUTE + "=true",
                HttpHandler.HANDLE_NO_AUTH + "=true"})
public class OAuth2Handler implements HttpHandler {
    public static final String ROOT_PATH = "/oauth2";
    private static final String AUTHORIZE_PATH = ROOT_PATH + "/authorize";
    private static final String CALLBACK_PATH = ROOT_PATH + "/callback";

    private static final String CSRF_COOKIE_NAME = "oauth2_csrf";
    private static final int CSRF_COOKIE_EXPIRE_SECONDS = 60;
    private static final String COOKIE_SAME_SITE = "Lax";

    @Reference
    private HttpClient httpClient;
    @Reference
    private Json json;
    @Reference
    private Crypto crypto;
    @Reference
    private TokenService tokenService;

    private boolean enabled;
    private String authorizationEndpoint;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String scope;
    private String callbackUrl;
    private String usernameField;
    private List<String> grantedRoles;

    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        enabled = configs.getOrDefault("oauth2_enabled", false);
        if (!enabled) return;

        authorizationEndpoint = configs.notBlank("oauth2_authorize_endpoint");
        clientId = configs.notBlank("oauth2_client_id");
        clientSecret = configs.notBlank("oauth2_client_secret"); // 安全考虑：此处不要解密
        tokenEndpoint = configs.notBlank("oauth2_token_endpoint");
        userinfoEndpoint = configs.notBlank("oauth2_userinfo_endpoint");
        usernameField = config.get("oauth2_userinfo_username_field");
        scope = config.get("oauth2_scope");
        // 角色 fail-closed：默认不授予任何角色，须由管理员显式配置 oauth2_roles 后 OAuth2 用户才有权限，防止"能通过外部 IdP 认证即获得平台管理权限"
        grantedRoles = configs.splitList("oauth2_roles");
        String thisServerUrl = configs.notBlank("oauth2_this_server");
        callbackUrl = thisServerUrl + CALLBACK_PATH;

        tokenCipher = crypto.getGlobalCipher();
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        if (!enabled) {
            response.status(404);
            return;
        }

        String path = request.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // 精确匹配，避免 /x/oauth2/authorize 之类误命中
        if (path.equals(AUTHORIZE_PATH)) {
            String state = UUID.randomUUID().toString().replace("-", ""); // state 随每次授权请求随机生成并存入 Cookie，回调时比对，借用此机制防登录 CSRF
            // state Cookie 必须 Lax：回调是 IdP 发起的跨站顶级 GET 导航，Strict 不会被携带，登录必然失败
            tokenService.sendCookie(CSRF_COOKIE_NAME, state, ROOT_PATH, CSRF_COOKIE_EXPIRE_SECONDS, COOKIE_SAME_SITE, request, response);
            response.redirect(buildAuthorizationUrl(state));
        } else if (path.equals(CALLBACK_PATH)) {
            callback(request, response);
        } else {
            response.status(400);
        }
    }

    private String buildAuthorizationUrl(String state) {
        StringBuilder url = new StringBuilder(authorizationEndpoint)
                .append("?response_type=code")
                .append("&client_id=").append(encode(clientId))
                .append("&redirect_uri=").append(encode(callbackUrl))
                .append("&state=").append(encode(state));
        if (scope != null) url.append("&scope=").append(encode(scope));
        return url.toString();
    }

    private String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void callback(HttpRequest request, HttpResponse response) throws Exception {
        String csrfToken = request.getParameter("state"); // OAUTH2 协议要求 state 必须传递
        if (csrfToken == null || !csrfToken.equals(request.getCookie(CSRF_COOKIE_NAME))) {
            response.status(400); // 拦截 csrf 攻击
            return;
        }
        // state 一次性使用：校验通过立即作废 Cookie，防止 csrfCookieExpireSeconds 秒窗口内被重复提交
        tokenService.sendCookie(CSRF_COOKIE_NAME, "", ROOT_PATH, 0, COOKIE_SAME_SITE, request, response);

        String code = request.getParameter("code");
        if (code == null) {
            response.status(400);
            return;
        }

        String accessToken = exchangeCodeForAccessToken(code, response);
        if (accessToken == null) return;

        String user = getUserInfoByAccessToken(accessToken);
        if (user == null) {
            response.status(500);
            return;
        }

        String[] roles = grantedRoles.toArray(new String[0]);
        tokenService.sendToken(user, roles, request, response);
        response.redirect("/web"); // 管控台前端入口，前端资源与接口分处不同路径，登录后须回到此处
    }

    private String exchangeCodeForAccessToken(String code, HttpResponse response) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("redirect_uri", callbackUrl); // RFC 6749：token 请求的 redirect_uri 必须与授权请求完全一致
        params.put("client_secret", tokenCipher.tryDecrypt(clientSecret, "qingzhou-auth.client_secret"));

        Response tokenResponse = httpClient.send(
                httpClient.buildRequest(tokenEndpoint).method(HttpMethod.POST).params(params));
        byte[] tokenBodyBytes = tokenResponse.getBody();
        if (tokenBodyBytes == null) {
            // 上游未返回响应体：固定文案回浏览器，避免 NPE 且不外泄内部信息
            response.status(403).send("token exchange failed");
            return null;
        }
        String tokenBodyText = new String(tokenBodyBytes, StandardCharsets.UTF_8);
        Map tokenBody = json.fromJson(tokenBodyText, Map.class);
        // 非 JSON 响应（如网关 HTML 错误页）fromJson 返回 null，须与 error 响应一并走失败路径，否则 NPE
        if (tokenResponse.getStatus() != 200 || tokenBody == null || tokenBody.get("error") != null) {
            // 固定文案回浏览器：上游 token 端点的原文可能含 IdP 内部信息，不应外泄
            response.status(403).send("token exchange failed");
            return null;
        }

        // 优先走 userinfo endpoint（由授权服务器校验 access_token，身份可信）
        return (String) tokenBody.get("access_token");
    }

    /**
     * 身份必须由授权服务器校验：只接受 userinfo endpoint 返回的 sub。
     * 不接受未验签的 id_token——其 payload 可任意伪造，据此登录等同于身份可被直接接管。
     */
    private String getUserInfoByAccessToken(String accessToken) throws Exception {
        if (accessToken != null && userinfoEndpoint != null && !userinfoEndpoint.isEmpty()) {
            Response resp = httpClient.send(httpClient.buildRequest(userinfoEndpoint)
                    .header("Authorization", "Bearer " + accessToken));
            if (resp.getStatus() == 200) {
                Map<String, Object> info = json.fromJson(new String(resp.getBody(), StandardCharsets.UTF_8), Map.class);
                return pickUsername(info);
            }
        }
        return null;
    }

    private String pickUsername(Map<String, Object> map) {
        String[] tryUserNames = {usernameField,
                "preferred_username",// OIDC 标准 scope=profile 时属性，服务器决定是否有值
                "name", // OIDC 标准 scope=profile 时属性，服务器决定是否有值
                "sub" // OIDC scope=openid 标准属性，一个id字符串
        };
        for (String field : tryUserNames) {
            if (field == null || field.isEmpty()) continue;
            String val = trimToNull(map.get(field));
            if (val != null) return val;
        }
        return null;
    }

    private String trimToNull(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }
}
