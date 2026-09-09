package qingzhou.auth;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.crypto.MessageDigest;
import qingzhou.crypto.TotpCipher;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

@Component(configurationPid = "qingzhou-auth", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=", HttpHandler.HANDLE_NO_AUTH + "=true"})
public class LoginHandler implements HttpHandler {
    private static final String LOGIN_PATH = "/auth/login";
    private static final String LOGOUT_PATH = "/auth/logout";

    @Reference
    private Crypto crypto;

    @Reference
    private TokenService tokenService;

    private String username;
    private String passwordDigest;
    private int maxFailures;
    private long lockMillis;
    private final Map<String, long[]> failures = new ConcurrentHashMap<>(); // ip -> {count, firstTime}

    private MessageDigest messageDigest;

    private boolean totpEnabled;
    private String totpSecret;
    private TotpCipher totpCipher;

    @Activate
    public void start(Map<String, String> config) {
        username = config.get("username");
        passwordDigest = config.get("password");
        maxFailures = parseInt(config.get("max_failures"), 5);
        lockMillis = parseInt(config.get("lock_seconds"), 300) * 1000L;

        messageDigest = crypto.getMessageDigest();

        totpEnabled = Boolean.parseBoolean(config.get("totp_enabled"));
        if (totpEnabled) {
            totpSecret = config.get("totp_secret"); // 安全考虑：此处不要解密
            totpCipher = crypto.getTotpCipher();
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // 精确匹配，避免 /x/auth/login 之类误命中
        if (path.equals(LOGIN_PATH)) {
            if (!"POST".equals(request.getMethod())) { // 防密码经 GET 进入 URL/访问日志
                response.status(405).sendFinish("method not allowed");
                return;
            }
            login(request, response);
        } else if (path.equals(LOGOUT_PATH)) {
            logout(response);
        } else {
            response.status400Finish();
        }
    }

    private void login(HttpRequest request, HttpResponse response) {
        String ip = request.getRemoteHost();
        if (isLocked(ip)) {
            response.status(429).sendFinish("too many login failures, try again later");
            return;
        }

        String reqUser = request.getParameter("username");
        String password = request.getParameter("password");
        boolean verified = Objects.equals(reqUser, username)
                && messageDigest.matches(password, passwordDigest);
        if (!verified) {
            recordFailure(ip);
            response.status(401).sendFinish("invalid username or password");
            return;
        }

        if (totpEnabled) {
            String code = request.getParameter("code");
            if (code == null || code.isEmpty()) {
                response.status(401).sendFinish("totp code required"); // 请求不完整，不计入失败
                return;
            }
            if (!verifyCode(code, response)) {
                recordFailure(ip);
                return;
            }
        }

        failures.remove(ip);
        response.contentTypeJsonUtf8().sendFinish("{\"token\":\"" + tokenService.createToken(reqUser) + "\"}");
    }

    private boolean verifyCode(String code, HttpResponse response) {
        try {
            if (totpCipher.verifyCode(crypto.getGlobalCipher().decrypt(totpSecret), code))
                return true;
        } catch (Exception e) {
            // 密钥非法或算法异常一律判为校验失败，避免异常穿透为 500
        }
        response.status(401).sendFinish("invalid totp code");
        return false;
    }

    private boolean isLocked(String ip) {
        long[] record = failures.get(ip);
        if (record == null) return false;

        if (record[0] < maxFailures) return false;

        long now = System.currentTimeMillis();
        return now - record[1] <= lockMillis;
    }

    private void recordFailure(String ip) {
        failures.compute(ip, (key, record) -> {
            long now = System.currentTimeMillis();
            if (record == null || now - record[1] > lockMillis) {
                return new long[]{1, now};
            }
            record[0]++;
            return record;
        });
        if (failures.size() > 10_000) { // 惰性清理过期记录，防止不同 IP 洪水导致内存膨胀
            long now = System.currentTimeMillis();
            failures.entrySet().removeIf(entry -> now - entry.getValue()[1] > lockMillis);
        }
    }

    private void logout(HttpResponse response) {
        // 无状态 token 无法服务端撤销，客户端删除凭据即完成登出
        response.sendFinish("ok");
    }

    private static int parseInt(String val, int defaultValue) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
