package qingzhou.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.Configs;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = TokenService.class)
public class TokenService {
    private static final String TOKEN_COOKIE_NAME = "auth_token";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String USER = "u";
    private static final String ROLES = "r";
    private static final String EXPIRE = "e";
    private static final String u = "u";
    private static final String r = "r";
    private static final String e = "e";
    private static final String v = "v";

    @Reference
    private Crypto crypto;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private long tokenExpireMillis;
    private Cipher tokenCipher;
    private String cookieAttrs;

    // 登出后吊销的 Bearer 令牌（内存态，随实例重启清空）；值为其最迟失效时刻，
    // 过期即从表中移除，防止集合无界增长。令牌本身短时效，重启不影响安全。
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();
    // 用户会话版本号：登出递增使该用户全部已发令牌失效（内存态随重启清空，令牌短时效兜底）
    private final Map<String, Integer> tokenVersions = new ConcurrentHashMap<>();

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        tokenExpireMillis = configs.getConfig("token_expire_seconds", 30 * 60) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
        // 跨站分离部署须配置 None（浏览器强制要求配合 Secure，即 HTTPS）
        String sameSite = configs.getConfig("cookie_samesite", "Lax");
        if (!"Lax".equalsIgnoreCase(sameSite) && !"Strict".equalsIgnoreCase(sameSite) && !"None".equalsIgnoreCase(sameSite)) {
            throw new IllegalArgumentException("cookie_samesite must be Lax, Strict or None, got: " + sameSite);
        }
        cookieAttrs = "; Path=/; HttpOnly; SameSite=" + sameSite;
    }

    public void sendToken(String user, String[] roles, HttpRequest request, HttpResponse response) {
        String token = createToken(user, roles);
        response.header("Set-Cookie", TOKEN_COOKIE_NAME + "=" + token
                        // Path 必须为 /：令牌既供 /web 下的前端使用，也供 /registry、/ai 等接口使用，限定前缀会导致接口收不到 Cookie 而报 401
                        + "; Path=/; HttpOnly; SameSite=Strict" + cookieSecure(request))
                .header("Cache-Control", "no-store")
                .redirect("/web"); // 管控台前端入口，前端资源与接口分处不同路径，登录后须回到此处
    /** 下发会话 cookie；响应形态（JSON/重定向）由调用方决定 */
    public void sendToken(HttpRequest request, String user, String[] roles, HttpResponse response) {
        response.header("Set-Cookie", setCookie(request, createToken(user, roles), ""))
                .header("Cache-Control", "no-store");
    }

    // Path 必须为 /：令牌既供 /web 下的前端使用，也供 /registry、/ai 等接口使用，限定前缀会导致接口收不到 Cookie 而报 401
    // Secure 依据实际连接协议而非配置字符串，见 HttpRequest#getScheme 约定
    private String setCookie(HttpRequest request, String value, String extraAttrs) {
        return TOKEN_COOKIE_NAME + "=" + value + extraAttrs + cookieAttrs
                + ("https".equalsIgnoreCase(request.getScheme()) ? "; Secure" : "");
    }

    public String retrieveToken(HttpRequest request) {
        String header = request.getHeader("Authorization");
        // RFC 6750：scheme 不区分大小写（部分客户端发小写 bearer）
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }

        return request.getCookie(TOKEN_COOKIE_NAME);
    }

    public void revokeToken(HttpRequest request, HttpResponse response) {
        String token = retrieveToken(request);
        if (token != null && !token.isEmpty()) {
            purgeExpiredRevokedTokens();
            // 服务端注销
            revokedTokens.put(token, System.currentTimeMillis() + tokenExpireMillis);
            // 通知客户端注销
            response.header("Set-Cookie", TOKEN_COOKIE_NAME
                            + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict" + cookieSecure(request))
                    .send("ok").finish();
        Object[] userRoles = verifyToken(retrieveToken(request));
        if (userRoles != null) {
            tokenVersions.merge((String) userRoles[0], 1, Integer::sum); // 服务端注销：使用户所有已发令牌失效
        }
        // 无论是否携带有效令牌，都通知客户端清除
        response.header("Set-Cookie", setCookie(request, "", "; Max-Age=0")).send("ok").finish();
    }

    /** 滑动续期：活跃会话剩余寿命不足一半时重签令牌，返回 Set-Cookie 值；无需续期返回 null */
    public String renewToken(HttpRequest request, String token) {
        Object[] userRoles = verifyToken(token);
        if (userRoles == null) return null;
        if ((Long) userRoles[2] - System.currentTimeMillis() > tokenExpireMillis / 2) return null;
        return setCookie(request, createToken((String) userRoles[0], (String[]) userRoles[1]), "");
    }

    // Secure 须依实际连接下发：配置写成 http 但经 TLS 反代访问时，按配置判定会漏发
    private String cookieSecure(HttpRequest request) {
        return "https".equals(request.getScheme()) ? "; Secure" : "";
    }

    // 令牌最长存活 tokenExpireMillis，吊销记录超过该时限即失去拦截意义，按需惰性清理
    private void purgeExpiredRevokedTokens() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : revokedTokens.entrySet()) {
            if (entry.getValue() <= now) revokedTokens.remove(entry.getKey());
        }
    }

    private String createToken(String user, String[] roles) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put(USER, user);
            payload.put(ROLES, roles == null ? new String[0] : roles);
            payload.put(EXPIRE, System.currentTimeMillis() + tokenExpireMillis);
            payload.put(u, user);
            payload.put(r, roles == null ? new String[0] : roles);
            payload.put(e, System.currentTimeMillis() + tokenExpireMillis);
            payload.put(v, tokenVersions.getOrDefault(user, 0));
            // 用结构化 JSON 承载，避免用户名/角色含分隔符时解析错位导致角色被篡改
            return tokenCipher.encrypt(json.toJson(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 校验通过返回 {user, roles, expiresAt}；无效/过期/已吊销返回 null */
    public Object[] verifyToken(String token) {
        if (token == null) return null;
        if (revokedTokens.containsKey(token)) return null;

        try {
            String payload = tokenCipher.decrypt(token);
            Map<String, Object> map = json.fromJson(payload, Map.class);
            if (map != null) {
                String user = (String) map.get(USER);
                if (user == null) return null;
                long expiresAt = ((Number) map.get(EXPIRE)).longValue();
                Number version = (Number) map.get(v);
                int tokenV = version == null ? 0 : version.intValue(); // 旧格式令牌视作版本 0
                if (tokenV != tokenVersions.getOrDefault(user, 0)) return null; // 已被登出吊销
                long expiresAt = ((Number) map.get(e)).longValue();
                if (expiresAt <= System.currentTimeMillis()) return null;
                String[] roles = toRoles(map.get(ROLES));
                return new Object[]{user, roles};
                return new Object[]{user, toRoles(map.get(r)), expiresAt};
            }
        } catch (Exception e) {
            // 校验失败是常见攻击面（伪造/篡改令牌），记录摘要便于审计；不打印令牌本身防止泄露凭据
            logger.warn("Token verification failed: " + e.getMessage());
        }
        return null;
    }

    private String[] toRoles(Object rolesObj) {
        if (rolesObj instanceof List) {
            List<String> list = (List<String>) rolesObj;
            return list.toArray(new String[0]);
        }
        if (rolesObj instanceof String) {
            String text = (String) rolesObj;
            if (!text.isEmpty()) {
                return new String[]{text};
            }
        }
        return new String[0];
    }
}
