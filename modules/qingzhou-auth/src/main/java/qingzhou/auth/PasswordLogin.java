package qingzhou.auth;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

@Component(configurationPid = "qingzhou-auth", property = HttpHandler.HANDLE_PATH + "=/auth")
public class PasswordLogin implements HttpHandler {
    static final String[] EXCLUDED_PATHS = {"/qingzhou-auth/auth/login", "/qingzhou-auth/auth/logout"};

    @Reference
    private Crypto crypto;

    private String username;
    private String password;
    private long tokenExpireMillis;
    private int maxFailures;
    private long lockMillis;
    private final Map<String, long[]> failures = new ConcurrentHashMap<>(); // ip -> {count, firstTime}

    @Activate
    public void start(Map<String, String> config) {
        username = config.get("username");
        password = config.get("password");
        tokenExpireMillis = Long.parseLong(config.get("token_expire_seconds")) * 1000;
        maxFailures = Integer.parseInt(config.get("max_failures"));
        lockMillis = Long.parseLong(config.get("lock_seconds")) * 1000;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String path = request.getPath();
        if (path.endsWith("/auth/login")) {
            if (!"POST".equals(request.getMethod())) { // 防密码经 GET 进入 URL/访问日志
                response.status(405).sendFinish("method not allowed");
                return;
            }
            login(request, response);
        } else if (path.endsWith("/auth/logout")) {
            logout(request, response);
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

        String user = request.getParameter("user");
        String password = request.getParameter("password");
        boolean verified = verifyCredentials(user, password);

        if (verified) {
            failures.remove(ip);
        } else {
            recordFailure(ip);
        }

        if (verified) {
            response.contentTypeJsonUtf8().sendFinish("{\"token\":\"" + createToken(user) + "\"}");
        } else {
            response.status(401).sendFinish("invalid user or password");
        }
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

    private boolean verifyCredentials(String user, String password) {
        return Objects.equals(username, user) && crypto.getMessageDigest().matches(password, this.password);
    }

    private String createToken(String user) {
//        Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
//        String payload = user + "|" + (System.currentTimeMillis() + tokenExpireMillis) + "|" + passwordFingerprint;
//        return URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
//                + "." + URL_ENCODER.encodeToString(sign(payload));
        return "todo";
    }

    private void logout(HttpRequest request, HttpResponse response) {
        String header = request.getHeader("Authorization");
        String BEARER = "Bearer ";
        if (header != null && header.startsWith(BEARER)) {
            // authLoginService.revoke(header.substring(BEARER.length()).trim());
        }
        response.sendFinish("ok");
    }
}
