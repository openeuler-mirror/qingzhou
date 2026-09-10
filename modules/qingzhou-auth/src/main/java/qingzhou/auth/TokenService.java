package qingzhou.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.AuthResult;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = TokenService.class)
public class TokenService {
    private static final String LOCAL_LOGIN = "|local";

    @Reference
    private Crypto crypto;

    private String username;
    private Set<String> roles = Collections.emptySet();
    private long tokenExpireMillis;
    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        tokenExpireMillis = Integer.parseInt(config.getOrDefault("token_expire_seconds", "" + 30 * 60)) * 1000L;
        tokenCipher = crypto.getGlobalCipher();
        username = config.get("username");
        String roleNames = config.get("roles");
        roles = roleNames == null ? Collections.emptySet() : Arrays.stream(roleNames.split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).collect(Collectors.toSet());
        if (roleNames != null && !roleNames.trim().isEmpty() && roles.isEmpty())
            throw new IllegalArgumentException("Non-empty role declaration contains no roles");
    }

    public String createToken(String user) {
        try {
            String payload = user + "|" + (System.currentTimeMillis() + tokenExpireMillis);
            // 放在有效期之后，区分本地登录与使用同一密钥签发的其他认证凭据。
            if (!roles.isEmpty() && user.equals(username)) payload += LOCAL_LOGIN;
            return tokenCipher.encrypt(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String verifyToken(String token) {
        try {
            String payload = tokenCipher.decrypt(token);
            if (payload.endsWith(LOCAL_LOGIN)) payload = payload.substring(0, payload.length() - LOCAL_LOGIN.length());
            int sep = payload.lastIndexOf('|');
            return System.currentTimeMillis() < Long.parseLong(payload.substring(sep + 1))
                    ? payload.substring(0, sep) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public AuthResult authenticate(String token) {
        String user = verifyToken(token);
        if (user == null) return AuthResult.reject("invalid token");
        try {
            boolean hasLocalRoles = user.equals(username) && !roles.isEmpty()
                    && tokenCipher.decrypt(token).endsWith(LOCAL_LOGIN);
            return AuthResult.pass(user, hasLocalRoles ? roles : Collections.emptySet());
        } catch (Exception e) {
            return AuthResult.reject("invalid token");
        }
    }
}
