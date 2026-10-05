package qingzhou.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.http.server.util.Configs;
import qingzhou.http.server.util.FailureLockout;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=" + AuthHandler.ROOT_PATH,
                HttpHandler.HANDLE_PATH_ABSOLUTE + "=true",
                HttpHandler.HANDLE_NO_AUTH + "=true"})
public class AuthHandler implements HttpHandler {
    public static final String ROOT_PATH = "/auth";
    private static final String LOGIN_PATH = ROOT_PATH + "/login";
    private static final String LOGOUT_PATH = ROOT_PATH + "/logout";

    @Reference
    private Crypto crypto;
    @Reference
    private TokenService tokenService;
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
        username = Configs.notBlank(config, "username");
        passwordDigest = Configs.notBlank(config, "password");
        String rolesStr = config.get("roles");
        if (rolesStr != null) {
            List<String> roleList = new ArrayList<>();
            for (String s : rolesStr.split(",")) {
                if (!s.trim().isEmpty()) {
                    roleList.add(s.trim());
                }
            }
            roles = roleList.toArray(new String[0]);
        }

        totpEnabled = Configs.getConfig(config, "totp_enabled", false);
        if (totpEnabled) {
            totpSecret = Configs.notBlank(config, "totp_secret"); // 安全考虑：此处不要解密
        }

        int maxFailures = Configs.atLeast(config, "max_failures", 1, 5);
        long lockMillis = Configs.atLeast(config, "lock_seconds", 1, 300) * 1000L;
        lockout = new FailureLockout(maxFailures, lockMillis, 10_000);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // 精确匹配，避免 /x/auth/login 之类误命中
        if (path.equals(LOGIN_PATH)) {
            if (!"POST".equals(request.getMethod())) { // 防密码经 GET 进入 URL/访问日志
                response.status(405).send("method not allowed").finish();
                return;
            }
            if (request.getRawUri().contains("?")) { // 凭据只允许经请求体传递，防止密码进入 URL/访问日志
                response.status(400).finish();
                return;
            }

            login(request, response);
        } else if (path.equals(LOGOUT_PATH)) {
            if (!"POST".equals(request.getMethod())) {
                response.status(405).send("method not allowed").finish();
                return;
            }
            logout(response);
        } else {
            response.status(400).finish();
        }
    }

    private void login(HttpRequest request, HttpResponse response) {
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
        response.header("Cache-Control", "no-store") // token 响应不得被缓存
                .contentTypeJsonUtf8()
                .send("{\"token\":\"" + tokenService.createToken(reqUser, roles) + "\"}").finish();
    }

    private boolean verifyUserPassword(String reqUser, String reqPass) {
        if (reqPass == null) reqPass = ""; // null 会让 matches 抛 NPE 变 500，泄露「用户名有效」信号；按空凭据处理

        return Objects.equals(reqUser, username)
                & // 用户名错误也执行等价慢哈希：抹平与「密码错误」的响应时序差，防用户名枚举
                crypto.getMessageDigest().matches(reqPass, passwordDigest);
    }

    private boolean verifyCode(String code, HttpResponse response) {
        try {
            if (crypto.getTotpCipher().verifyCode(
                    crypto.getGlobalCipher().tryDecrypt(totpSecret, "qingzhou-auth.auth_totp_secret"),
                    code)) {
                return true;
            }
        } catch (Exception e) {
            logger.error("TOTP authentication failed. Please verify whether the secret key is configured correctly.");
        }
        response.status(401).send("invalid totp code").finish();
        return false;
    }

    private void logout(HttpResponse response) {
        response.header("Set-Cookie", qingzhou.auth.oauth2.OAuth2Handler.TOKEN_COOKIE_NAME
                        + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax")
                .send("ok").finish();
    }
}
