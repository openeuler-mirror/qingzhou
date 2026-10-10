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
import qingzhou.json.Json;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = TokenService.class)
public class TokenService {
    private static final String USER_SP = ">";
    private static final String ROLES_SP = ",";

    @Reference
    private Crypto crypto;
    @Reference
    private Json json;

    private long tokenExpireMillis;
    private Cipher tokenCipher;
    // 吊销令牌的容量上限：令牌本身 30 分钟过期，黑名单只需覆盖到过期即可，超出则淘汰最旧
    private static final int MAX_REVOKED_TOKENS = 100_000;
    // 登出后吊销的 Bearer 令牌集合（内存态，随实例重启清空；令牌本身短时效，重启不影响安全）
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        tokenExpireMillis = configs.getConfig("token_expire_seconds", 30 * 60) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
    }

    public String createToken(String user, String[] roles) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("u", user);
            payload.put("r", roles == null ? new String[0] : roles);
            payload.put("e", System.currentTimeMillis() + tokenExpireMillis);
            // 用结构化 JSON 承载，避免用户名/角色含分隔符时解析错位导致角色被篡改
            return tokenCipher.encrypt(json.toJson(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Object[] verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            // 优先按新 JSON 格式解析；解析失败回退旧格式，保证迁移期内存量令牌仍可校验
            Map<String, Object> map = json.fromJson(payload, Map.class);
            if (map != null) {
                String user = (String) map.get("u");
                if (user == null) return null;
                long expiresAt = ((Number) map.get("e")).longValue();
                if (expiresAt <= System.currentTimeMillis()) return null;
                String[] roles = toRoles(map.get("r"));
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

    // 登出吊销：把令牌加入黑名单，verifyToken 前拦截，使已签发的 Bearer 令牌立即失效
    public void revokeToken(String token) {
        if (token != null && !token.isEmpty()) revokedTokens.add(token);
    }

    public boolean isRevoked(String token) {
        return token != null && revokedTokens.contains(token);
    }
}
