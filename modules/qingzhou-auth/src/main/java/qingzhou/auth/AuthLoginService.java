package qingzhou.auth;

/**
 * 凭据登录服务：凭据校验、会话签发与校验。
 * 供登录/登出端点与 OAuth2 认证器依赖，与具体认证实现解耦。
 */
public interface AuthLoginService {

    boolean verifyCredentials(String user, String password);

    /**
     * 为用户签发签名 token（Bearer / 会话 cookie 通用）。
     */
    String createToken(String user);

    /**
     * 校验 token 并返回对应用户，无效返回 null。
     */
    String verifyToken(String token);

    void revoke(String token);
}
