package qingzhou.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.logger.Logger;

/**
 * 登录/登出接口：POST /auth/login、POST /auth/logout（含 IP 维度防暴力破解）。
 */
@Component(immediate = true, configurationPid = "qingzhou-auth", property = HttpHandler.HANDLE_PATH + "=")
public class AuthHandler implements HttpHandler {
    private static final String BEARER = "Bearer ";

    @Reference
    private AuthLoginService authLoginService;
    @Reference
    private Logger logger;

    private int maxFailures = 5;
    private long lockMillis = 300_000;
    private final Map<String, long[]> failures = new ConcurrentHashMap<>(); // ip -> {count, firstTime}

    @Activate
    public void start(Map<String, String> config) {
        maxFailures = (int) parsePositive(config.get("max_failures"), maxFailures);
        lockMillis = parsePositive(config.get("lock_seconds"), 300) * 1000;
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
        if (!authLoginService.verifyCredentials(user, password)) {
            recordFailure(ip);
            logger.info("login failed, ip: " + ip);
            response.status(401).sendFinish("invalid user or password");
            return;
        }
        failures.remove(ip);
        response.contentTypeJsonUtf8().sendFinish("{\"token\":\"" + authLoginService.createToken(user) + "\"}");
    }

    private void logout(HttpRequest request, HttpResponse response) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            authLoginService.revoke(header.substring(BEARER.length()).trim());
        }
        response.sendFinish("ok");
    }

    private boolean isLocked(String ip) {
        long[] record = failures.get(ip);
        if (record == null) return false;
        long now = System.currentTimeMillis();
        if (now - record[1] > lockMillis) {
            failures.remove(ip);
            return false;
        }
        return record[0] >= maxFailures;
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

    private static long parsePositive(String val, long defaultValue) {
        try {
            long parsed = Long.parseLong(val);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
