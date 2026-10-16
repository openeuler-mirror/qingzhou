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

    @Reference
    private Crypto crypto;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private long tokenExpireMillis;
    private Cipher tokenCipher;
    private String cookieSecure;
    private String cookieSamesite;

    // 登出后吊销的 Bearer 令牌（内存态，随实例重启清空）；值为其最迟失效时刻，
    // 过期即从表中移除，防止集合无界增长。令牌本身短时效，重启不影响安全。
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        tokenExpireMillis = configs.getOrDefault("token_expire_seconds", 30 * 60) * 1000L;
        cookieSecure = configs.enumValue("cookie_secure", new String[]{"true", "false", "auto"}, "auto");
        // 跨站分离部署须配置 None（浏览器强制要求配合 Secure，即 HTTPS）
        cookieSamesite = configs.enumValue("cookie_samesite", new String[]{"Lax", "Strict", "None"}, "Strict");

        tokenCipher = crypto.getGlobalCipher();
    }

    public void sendToken(String user, String[] roles, HttpRequest request, HttpResponse response) {
        String token = createToken(user, roles);

        // Path 必须为 /：令牌既供 /web 下的前端使用，也供 /registry、/ai 等接口使用，限定前缀会导致接口收不到 Cookie 而报 401
        sendCookie(TOKEN_COOKIE_NAME, token, "/", (int) (tokenExpireMillis / 1000), cookieSamesite, request, response);
    }

    private String createToken(String user, String[] roles) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put(USER, user);
            payload.put(ROLES, roles == null ? new String[0] : roles);
            payload.put(EXPIRE, System.currentTimeMillis() + tokenExpireMillis);
            // 用结构化 JSON 承载，避免用户名/角色含分隔符时解析错位导致角色被篡改
            return tokenCipher.encrypt(json.toJson(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void sendCookie(String name, String value, String path, int age, String sameSite, HttpRequest request, HttpResponse response) {
        String cookie = "; Path=" + path + "; HttpOnly; SameSite=" + sameSite;

        // Max-Age=0 或负数：表示立即过期，常用于删除 Cookie
        cookie += "; Max-Age=" + age;

        // Secure 须依实际连接下发：配置写成 http 但经 TLS 反代访问时按 scheme 判定会漏发，
        // 可用 cookie_secure=true/false 显式覆盖（auto 按请求 scheme）
        if ("true".equals(this.cookieSecure)) {
            cookie += "; Secure";
        } else if ("auto".equals(this.cookieSecure)) {
            // auto 时按请求 scheme 判定：https 时 Secure，http 时不设
            if ("https".equals(request.getScheme())) {
                cookie += "; Secure";
            }
        }

        response.header("Set-Cookie", name + "=" + value + cookie)
                .header("Cache-Control", "no-store");
    }

    public String retrieveToken(HttpRequest request) {
        // 优先从 Cookie 中获取
        String token = request.getCookie(TOKEN_COOKIE_NAME);

        // 从 Bearer 中获取
        if (token == null || token.isEmpty()) {
            String header = request.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                token = header.substring(BEARER_PREFIX.length()).trim();
            }
        }

        return token;
    }

    public void revokeToken(HttpRequest request, HttpResponse response) {
        String token = retrieveToken(request);
        if (token != null && !token.isEmpty()) {
            // 仅吊销真实有效的令牌：伪造令牌不入表，防止未认证请求高频调用灌爆吊销表（内存 DoS）
            Map<String, Object> validToken = parseToken(token);
            if (validToken != null) {
                // 令牌最长存活 tokenExpireMillis，吊销记录超过该时限即失去拦截意义，按需惰性清理
                long now = System.currentTimeMillis();
                for (Map.Entry<String, Long> entry : revokedTokens.entrySet()) {
                    if (entry.getValue() < now) revokedTokens.remove(entry.getKey());
                }

                // 服务端注销
                revokedTokens.put(token, now + tokenExpireMillis);
            }

            // 通知客户端注销
            sendCookie(TOKEN_COOKIE_NAME, "", "/", 0, cookieSamesite, request, response);
            response.send("ok").finish();
        }
    }

    private Map<String, Object> parseToken(String token) {
        try {
            Map<String, Object> map = json.fromJson(tokenCipher.decrypt(token), Map.class);
            if (map != null && map.get(EXPIRE) instanceof Number) {
                return map;
            }
        } catch (Exception e) {
            logger.warn("Token verification failed: " + e.getMessage());
        }
        return null;
    }

    public Object[] verifyToken(String token, HttpRequest request, HttpResponse response) {
        if (token == null) return null;
        if (revokedTokens.containsKey(token)) return null;

        Map<String, Object> map = parseToken(token);
        if (map == null) return null;

        if (!(map.get(EXPIRE) instanceof Number)) return null;
        long expiresAt = ((Number) map.get(EXPIRE)).longValue();
        long left = expiresAt - System.currentTimeMillis();
        if (left <= 0) return null;

        String user = (String) map.get(USER);
        String[] roles = toRoles(map.get(ROLES));

        // 滑动续期：活跃会话剩余寿命不足一半时重签令牌
        if (left < tokenExpireMillis / 2) {
            sendToken(user, roles, request, response);
        }

        return new Object[]{user, roles};
    }

    private String[] toRoles(Object rolesObj) {
        if (rolesObj instanceof List) {
            List<String> list = (List<String>) rolesObj;
            return list.toArray(new String[0]);
        }
        if (rolesObj instanceof String) {
            String text = (String) rolesObj;
            return new String[]{text};
        }
        return new String[0];
    }
}
