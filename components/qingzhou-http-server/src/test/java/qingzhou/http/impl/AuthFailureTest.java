package qingzhou.http.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import reactor.netty.http.server.HttpServerRequest;

/**
 * 认证失败节流的单元测试：不起服务，直接驱动 AuthManager.doAuth。
 * <p>
 * 认证失败与已被锁定都返回 false，无法从返回值区分，故统一以「认证器被调用的次数」判定：
 * 一旦锁定，doAuth0 不再执行，认证器也就不会被调用。
 */
public class AuthFailureTest {
    private static final int MAX_TRACKED_HOSTS = 10000; // 与 AuthManager 保持一致

    @Test
    public void failuresReachMax_nextRequest_skipsAuthenticator() throws Exception {
        AuthManager authManager = manager(2, 60);
        AtomicInteger calls = new AtomicInteger();
        authManager.addAuthenticator(request -> {
            calls.incrementAndGet();
            return AuthResult.reject("no");
        });

        for (int i = 0; i < 3; i++) authManager.doAuth(request("10.0.0.1"), entry());

        Assert.assertEquals(calls.get(), 2); // 第三次已被锁定，认证器不再执行
    }

    @Test
    public void authenticationPassed_failureRecord_cleared() throws Exception {
        AuthManager authManager = manager(2, 60);
        AtomicInteger calls = new AtomicInteger();
        authManager.addAuthenticator(request -> {
            int n = calls.incrementAndGet();
            return n == 2 ? AuthResult.pass("u", null) : AuthResult.reject("no"); // 第二次放行，用于验证计数被清零
        });

        authManager.doAuth(request("10.0.0.2"), entry());
        Assert.assertTrue(authManager.doAuth(request("10.0.0.2"), entry()));

        for (int i = 0; i < 3; i++) authManager.doAuth(request("10.0.0.2"), entry());

        // 计数未清零的话，第 4 次即被锁定，认证器只会调用 3 次
        Assert.assertEquals(calls.get(), 4);
    }

    @Test
    public void manyDistinctHosts_failureRecords_stayBounded() throws Exception {
        AuthManager authManager = manager(100, 60);
        authManager.addAuthenticator(request -> AuthResult.reject("no"));

        int hosts = MAX_TRACKED_HOSTS + 2000;
        for (int i = 0; i < hosts; i++) {
            authManager.doAuth(request("10.1." + (i / 254) + "." + (i % 254 + 1)), entry());
        }

        int tracked = trackedHosts(authManager);
        Assert.assertTrue(tracked <= MAX_TRACKED_HOSTS + 1000,
                "failure records must stay bounded, was " + tracked);
    }

    private static AuthManager manager(int maxFailures, int windowSeconds) {
        AuthManager authManager = new AuthManager();
        Map<String, String> config = new HashMap<>();
        config.put("auth_fail_max", String.valueOf(maxFailures));
        config.put("auth_fail_window", String.valueOf(windowSeconds));
        authManager.init(config);
        return authManager;
    }

    private static HandlerManager.HandlerEntry entry() {
        return new HandlerManager.HandlerEntry((HttpRequest request, HttpResponse response) -> {
        }, false);
    }

    // 仅 remoteAddress 会被 doAuth 用到，其余方法返回 null 即可
    private static HttpRequestImpl request(String host) {
        HttpServerRequest stub = (HttpServerRequest) Proxy.newProxyInstance(
                HttpServerRequest.class.getClassLoader(),
                new Class<?>[]{HttpServerRequest.class},
                (proxy, method, args) -> "remoteAddress".equals(method.getName())
                        ? new InetSocketAddress(host, 12345) : null);
        return new HttpRequestImpl(stub, "/x");
    }

    private static int trackedHosts(AuthManager authManager) throws Exception {
        Field field = AuthManager.class.getDeclaredField("authFailures");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(authManager)).size();
    }
}
