package qingzhou.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import qingzhou.crypto.Crypto;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpAuthenticator;
import qingzhou.http.server.HttpRequest;

/**
 * 基于 HMAC-SHA256 无状态签名 Token 的认证器与登录服务。
 * Token 格式：base64url(payload).base64url(hmac(payload, secret))，payload = user|expireMillis|密码指纹。
 * 密码指纹 = sha256(密码摘要)，改密码配置后旧 token 全部立即失效（无状态全局失效）。
 */
@Component(immediate = true, configurationPid = "qingzhou-auth",
        service = {HttpAuthenticator.class, AuthLoginService.class})
public class TokenAuthenticator implements HttpAuthenticator, AuthLoginService {
    private static final String BEARER = "Bearer ";
    private static final String[] EXCLUDED_PATHS = {"/auth/login", "/auth/logout"};
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    @Reference
    private Crypto crypto;

    private String username;
    private SecretKeySpec secretKey;
    private String passwordDigest; // 格式：算法$盐$迭代次数$摘要，由 qingzhou-crypto 生成/校验
    private String passwordFingerprint; // 改密即全局失效
    private long tokenExpireMillis;

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>(); // 已登出 token -> 过期时间

    @Activate
    public void start(Map<String, String> config) {
        username = get(config, "user", "admin");
        String secretConfig = get(config, "secret", "");
        byte[] secret = secretConfig.isEmpty() ? randomBytes(32) : secretConfig.getBytes(StandardCharsets.UTF_8);
        secretKey = new SecretKeySpec(secret, "HmacSHA256");
        tokenExpireMillis = parsePositive(config.get("token_expire_seconds"), 3600) * 1000;
        String password = get(config, "password", "admin");
        passwordDigest = password.startsWith("digest:")
                ? password.substring("digest:".length()) // 预生成摘要：算法$盐$迭代次数$摘要
                : crypto.getMessageDigest().digest(password, "SHA-256", 16,
                (int) parsePositive(config.get("password_iterations"), 2));
        passwordFingerprint = crypto.getMessageDigest().sha256(passwordDigest);
    }

    @Override
    public String[] excludedPaths() {
        return EXCLUDED_PATHS;
    }

    @Override
    public boolean verifyCredentials(String user, String password) {
        return Objects.equals(username, user) && crypto.getMessageDigest().matches(password, passwordDigest);
    }

    @Override
    public String createToken(String user) {
        String payload = user + "|" + (System.currentTimeMillis() + tokenExpireMillis) + "|" + passwordFingerprint;
        return URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + URL_ENCODER.encodeToString(sign(payload));
    }

    @Override
    public String verifyToken(String token) {
        if (blacklist.containsKey(token)) return null;
        try {
            int dot = token.indexOf('.');
            if (dot <= 0) return null;
            String payload = new String(URL_DECODER.decode(token.substring(0, dot)),
                    StandardCharsets.UTF_8);
            if (!constantTimeEquals(sign(payload), URL_DECODER.decode(token.substring(dot + 1)))) {
                return null;
            }
            int firstSep = payload.indexOf('|');
            int lastSep = payload.lastIndexOf('|');
            if (firstSep <= 0 || !payload.substring(lastSep + 1).equals(passwordFingerprint)) {
                return null; // 改密后旧 token 立即失效
            }
            long expire = Long.parseLong(payload.substring(firstSep + 1, lastSep));
            return System.currentTimeMillis() > expire ? null : payload.substring(0, firstSep);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void revoke(String token) {
        try {
            int dot = token.indexOf('.');
            String payload = new String(URL_DECODER.decode(token.substring(0, dot)),
                    StandardCharsets.UTF_8);
            // 只吊销签名有效的 token，防止伪造 token（可携带超远过期时间）污染黑名单
            if (constantTimeEquals(sign(payload), URL_DECODER.decode(token.substring(dot + 1)))) {
                blacklist.put(token, Long.parseLong(payload.substring(payload.indexOf('|') + 1, payload.lastIndexOf('|'))));
            }
            if (blacklist.size() > 1000) { // 惰性清理过期条目，避免无限增长
                long now = System.currentTimeMillis();
                blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public AuthResult authenticate(HttpRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return AuthResult.missing(); // 无 Bearer 凭据为中性结果，交由其他认证器（如 OAuth2）决定
        }
        String user = verifyToken(header.substring(BEARER.length()).trim());
        return user != null ? AuthResult.pass(user) : AuthResult.reject("invalid token");
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String get(Map<String, String> config, String key, String defaultValue) {
        String val = config.get(key);
        return val == null || val.isEmpty() ? defaultValue : val;
    }

    private static long parsePositive(String val, long defaultValue) {
        try {
            long parsed = Long.parseLong(val);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
        return result == 0;
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
