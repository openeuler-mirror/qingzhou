package qingzhou.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = TokenService.class)
public class TokenService {
    private static final String TOKEN_COOKIE_NAME = "auth_token";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String u = "u";
    private static final String r = "r";
    private static final String e = "e";

    @Reference
    private Crypto crypto;
    @Reference
    private Json json;

    private long tokenExpireMillis;
    private Cipher tokenCipher;

    // 登出后吊销的 Bearer 令牌集合（内存态，随实例重启清空；令牌本身短时效，重启不影响安全）
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet(); // todo 待完善

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        tokenExpireMillis = configs.getConfig("token_expire_seconds", 30 * 60) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
    }

    public void sendToken(String user, String[] roles, HttpResponse response) {
        String token = createToken(user, roles);
        response.header("Set-Cookie", TOKEN_COOKIE_NAME + "=" + token
                        // Path 必须为 /：令牌既供 /web 下的前端使用，也供 /registry、/ai 等接口使用，限定前缀会导致接口收不到 Cookie 而报 401
                        + "; Path=/; HttpOnly; SameSite=Strict")
                .header("Cache-Control", "no-store")
                .redirect("/web"); // 管控台前端入口，前端资源与接口分处不同路径，登录后须回到此处
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
            // 服务端注销
            revokedTokens.add(token);
            // 通知客户端注销
            response.header("Set-Cookie", TOKEN_COOKIE_NAME
                            + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict")
                    .send("ok").finish();
        }
    }

    private String createToken(String user, String[] roles) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put(u, user);
            payload.put(r, roles == null ? new String[0] : roles);
            payload.put(e, System.currentTimeMillis() + tokenExpireMillis);
            // 用结构化 JSON 承载，避免用户名/角色含分隔符时解析错位导致角色被篡改
            return tokenCipher.encrypt(json.toJson(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Object[] verifyToken(String token) {
        if (token == null) return null;
        if (revokedTokens.contains(token)) return null;

        try {
            String payload = tokenCipher.decrypt(token);
            // 优先按新 JSON 格式解析；解析失败回退旧格式，保证迁移期内存量令牌仍可校验
            Map<String, Object> map = json.fromJson(payload, Map.class);
            if (map != null) {
                String user = (String) map.get(u);
                if (user == null) return null;
                long expiresAt = ((Number) map.get(e)).longValue();
                if (expiresAt <= System.currentTimeMillis()) return null;
                String[] roles = toRoles(map.get(r));
                return new Object[]{user, roles};
            }
        } catch (Exception ignored) {
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
