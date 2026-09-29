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
import qingzhou.logger.impl.LoggerImpl;
import reactor.netty.http.server.HttpServerRequest;

import static qingzhou.http.impl.HttpServerImplTest.setField;

/**
 * 认证失败节流的单元测试：不起服务，直接驱动 AuthManager.doAuth。
 * <p>
 * 认证失败与已被锁定都返回 false，无法从返回值区分，故统一以「认证器被调用的次数」判定：
 * 一旦锁定，authenticateWithHandler 不再执行，认证器也就不会被调用。
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
    public void capacityOverflow_evictsOldestFailures() throws Exception {
        AuthManager authManager = manager(2, 60);
        Map<String, AuthManager.AuthFailure> failures = failuresOf(authManager);

        // 时间戳落在窗口内（过早会被 removeIf 当作过期项先行回收）且严格递增，保证淘汰顺序确定
        long baseTime = System.currentTimeMillis() - 30_000;
        for (int i = 0; i < 10_004; i++) {
            AuthManager.AuthFailure failure = new AuthManager.AuthFailure();
            setLastFailureAt(failure, baseTime + i);
            failures.put(host(i), failure);
        }

        // 第 10005 个来源失败：恰好跨过容量上限，触发一次水位淘汰
        authManager.doAuth(request(host(10_004)), entry());

        Assert.assertEquals(failures.size(), 9_000); // 一次淘汰到低水位
        for (int i = 0; i < 1_005; i++) {
            Assert.assertFalse(failures.containsKey(host(i)), "oldest source must be evicted: " + host(i));
        }
        for (int i = 1_005; i < 10_004; i += 997) {
            Assert.assertTrue(failures.containsKey(host(i)), "newer source must survive: " + host(i));
        }
        Assert.assertTrue(failures.containsKey(host(10_004)), "newest source must survive");
    }

    // 用 IP 字面量构造 host：避免 InetSocketAddress 对主机名发起 DNS 解析
    private static String host(int index) {
        return "10." + (index / 65536 % 256) + "." + (index / 256 % 256) + "." + (index % 256);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AuthManager.AuthFailure> failuresOf(AuthManager authManager) throws Exception {
        Field field = AuthManager.class.getDeclaredField("authFailures");
        field.setAccessible(true);
        return (Map<String, AuthManager.AuthFailure>) field.get(authManager);
    }

    private static void setLastFailureAt(AuthManager.AuthFailure failure, long lastFailureAt) throws Exception {
        Field field = AuthManager.AuthFailure.class.getDeclaredField("lastFailureAt");
        field.setAccessible(true);
        field.setLong(failure, lastFailureAt);
    }

    private static AuthManager manager(int maxFailures, int windowSeconds) throws Exception {
        AuthManager authManager = new AuthManager();
        Map<String, String> config = new HashMap<>();
        config.put("auth_fail_max", String.valueOf(maxFailures));
        config.put("auth_fail_window", String.valueOf(windowSeconds));
        LoggerImpl logger = new LoggerImpl();
        setField(authManager, "logger", logger);
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
}
