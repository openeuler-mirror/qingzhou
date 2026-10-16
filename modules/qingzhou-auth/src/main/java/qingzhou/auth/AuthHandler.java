package qingzhou.auth;

import java.util.*;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.Configs;
import qingzhou.common.FailureLockout;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + AuthHandler.ROOT_PATH,
                HttpHandler.HANDLE_PATH_ABSOLUTE + "=true",
                HttpHandler.HANDLE_NO_AUTH + "=true"})
public class AuthHandler implements HttpHandler {
    public static final String ROOT_PATH = "/auth";
    private static final String LOGIN_PATH = ROOT_PATH + "/login";
    private static final String LOGOUT_PATH = ROOT_PATH + "/logout";
    private static final String SESSION_PATH = ROOT_PATH + "/session";
    // 凭据长度上限：慢哈希成本随长度增长，免认证端点上超长凭据即 CPU 放大攻击，超长直接按无效处理
    private static final int MAX_CREDENTIAL_CHARS = 1024;

    @Reference
    private Crypto crypto;
    @Reference
    private TokenService tokenService;
    @Reference
    private Json json;
    @Reference
    private Logger logger;

    private String username;
    private String passwordDigest;
    private String[] roles;

    private boolean totpEnabled;
    private String totpSecret;

    private FailureLockout lockout;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        username = configs.notBlank("username");
        passwordDigest = configs.notBlank("password");
        String rolesStr = config.get("roles");
        if (rolesStr != null && !rolesStr.trim().isEmpty()) {
            List<String> roleList = new ArrayList<>();
            for (String s : rolesStr.split(",")) {
                if (!s.trim().isEmpty()) {
                    roleList.add(s.trim());
                }
            }
            roles = roleList.toArray(new String[0]);
        }

        totpEnabled = configs.getOrDefault("totp_enabled", false);
        if (totpEnabled) {
            totpSecret = configs.notBlank("totp_secret"); // 安全考虑：此处不要解密
        }

        int maxFailures = configs.atLeast("max_failures", 1, 5);
        long lockMillis = configs.atLeast("lock_seconds", 1, 300) * 1000L;
        lockout = new FailureLockout(maxFailures, lockMillis, 10_000);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        String path = request.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // 精确匹配，避免 /x/auth/login 之类误命中
        boolean isPost = "POST".equals(request.getMethod());
        switch (path) {
            case LOGIN_PATH:
                // 凭据只允许经 POST 请求体传递
                if (!isPost || request.getRawUri().contains("?")) { // 防止密码进入 URL/访问日志；
                    response.status(400).finish();
                    return;
                }
                login(request, response);
                break;
            case LOGOUT_PATH:
                // 登出限定 POST：防跨站 GET 触发登出（logout CSRF）
                if (!isPost) {
                    response.status(400).finish();
                    return;
                }
                logout(request, response);
                break;
            case SESSION_PATH:
                session(request, response);
                break;
        }
    }

    private void login(HttpRequest request, HttpResponse response) throws Exception {
        String reqUser = request.getParameter("username");
        String ip = request.getRemoteHost();
        if (reqUser != null && reqUser.length() > 128) {
            response.status(401).send("invalid username or password").finish();
            return;
        }
        String lockKey = ip + '\0' + (reqUser == null ? "" : reqUser);
        if (lockout.isLocked(lockKey)) {
            response.status(429).send("too many login failures, try again later").finish();
            return;
        }

        String reqPass = request.getParameter("password");
        boolean verified = verifyUserPassword(reqUser, reqPass);
        if (!verified) {
            lockout.recordFailure(lockKey);
            response.status(401).send("invalid username or password").finish();
            return;
        }

        if (totpEnabled) {
            String code = request.getParameter("code");
            if (code == null || code.isEmpty()) {
                response.status(401).send("totp code required").finish(); // 请求不完整，不计入失败
                return;
            }
            if (!verifyCode(code, response)) {
                lockout.recordFailure(lockKey);
                return;
            }
        }

        lockout.clear(lockKey);
        tokenService.sendToken(reqUser, roles, request, response);

        response.finish();
    }

    /**
     * 会话探测：前端路由守卫与用户名展示的唯一依据
     * （会话 cookie 为 HttpOnly，前端无法自行判断登录态，OAuth2 回调后更是如此）
     */
    private void session(HttpRequest request, HttpResponse response) throws Exception {
        Object[] userRoles = tokenService.verifyToken(tokenService.retrieveToken(request), request, response);
        if (userRoles == null) {
            response.status(401).finish();
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("username", userRoles[0]);
        body.put("roles", userRoles[1]);
        response.contentTypeJsonUtf8().send(json.toJson(body)).finish();
    }

    private boolean verifyUserPassword(String reqUser, String reqPass) {
        if (reqPass == null) reqPass = ""; // null 会让 matches 抛 NPE 变 500，泄露「用户名有效」信号；按空凭据处理
        if (reqPass.length() > MAX_CREDENTIAL_CHARS) return false;

        return Objects.equals(reqUser, username)
                & // 用户名错误也执行等价慢哈希：抹平与「密码错误」的响应时序差，防用户名枚举
                crypto.getMessageDigest().matches(reqPass, passwordDigest);
    }

    private boolean verifyCode(String code, HttpResponse response) {
        try {
            if (crypto.getTotpCipher().verifyCode(
                    crypto.getGlobalCipher().tryDecrypt(totpSecret, "qingzhou-auth.totp_secret"),
                    code)) {
                return true;
            }
        } catch (Exception e) {
            logger.error("TOTP authentication failed. Please verify whether the secret key is configured correctly.");
        }
        response.status(401).send("invalid totp code").finish();
        return false;
    }

    private void logout(HttpRequest request, HttpResponse response) {
        tokenService.revokeToken(request, response);
    }
}
