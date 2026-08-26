package qingzhou.http.server;

import java.util.Objects;

/**
 * 认证结果：通过 / 凭据无效 / 引导认证（重定向）/ 无凭据。
 * 多认证器组合规则：任一 pass 放行；任一 reject 拒绝（401）；否则任一 challenge 重定向（302）；全部 missing 拒绝（401）。
 */
public final class AuthResult {
    private static final AuthResult PASS = new AuthResult(null, null, null, false);
    private static final AuthResult MISSING = new AuthResult(null, null, null, true);

    private final String reason;     // 拒绝原因，null 表示非拒绝
    private final String location;   // 质询重定向地址，非 null 表示需重定向
    private final Object principal;  // 认证主体（用户名等），仅 pass 时有意义
    private final boolean missing;   // 无凭据中性结果

    private AuthResult(String reason, String location, Object principal, boolean missing) {
        this.reason = reason;
        this.location = location;
        this.principal = principal;
        this.missing = missing;
    }

    public static AuthResult pass() {
        return PASS;
    }

    /**
     * 认证通过并携带主体身份，业务 Handler 可通过 HttpRequest.getAttribute 读取。
     */
    public static AuthResult pass(Object principal) {
        return new AuthResult(null, null, principal, false);
    }

    /**
     * 请求未携带本认证器支持的凭据（中性结果，交由其他认证器决定）。
     */
    public static AuthResult missing() {
        return MISSING;
    }

    /**
     * 凭据存在但无效。
     */
    public static AuthResult reject(String reason) {
        return new AuthResult(Objects.requireNonNull(reason, "reason"), null, null, false);
    }

    /**
     * 客户端需重定向到 location 完成认证（如 OAuth2 授权服务器）。
     */
    public static AuthResult challenge(String location) {
        return new AuthResult(null, Objects.requireNonNull(location, "location"), null, false);
    }

    public boolean isPassed() {
        return !missing && reason == null && location == null;
    }

    public boolean isMissing() {
        return missing;
    }

    public boolean isRejected() {
        return reason != null;
    }

    public boolean isChallenge() {
        return location != null;
    }

    public String getReason() {
        return reason;
    }

    public String getLocation() {
        return location;
    }

    public Object getPrincipal() {
        return principal;
    }
}
