package qingzhou.http.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;

import static qingzhou.http.impl.HttpServerImpl.getConfig;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    private static final int MAX_TRACKED_HOSTS = 10000; // 来源数量上限，超限时淘汰最久未失败者

    @Reference
    private Logger logger;

    private final PendingLogs pendingLogs = new PendingLogs();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性
    private volatile int maxAuthFailures; // 窗口内允许的认证失败次数
    private volatile int authFailWindowMillis;
    private final Map<String, AuthFailure> authFailures = new ConcurrentHashMap<>();

    @Activate
    public void init(Map<String, String> config) { // 暂存日志的回放与动态绑定的互斥由 PendingLogs 自身同步保证
        maxAuthFailures = getConfig(config, "auth_fail_max", 10);
        authFailWindowMillis = getConfig(config, "auth_fail_window", 60) * 1000;

        pendingLogs.replay(logger);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeAuthenticator")
    public void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);

        pendingLogs.log(logger, "http authenticator registered: " + authenticator.getClass().getName());
    }

    public void removeAuthenticator(Authenticator authenticator) {
        authenticators.remove(authenticator);
    }

    /**
     * 安全认证：多认证器聚合——任一 PASS 即放行；首个显式 REJECT 优先拒绝（客户端已出示凭据，须明确告知 401）；
     * 全部既未 PASS 也未 REJECT 时按无凭据拒绝。
     * 免认证标记取自 handlerEntry（随注册路径）；不提供全局免认证开关，避免一处配置放通全站。
     */
    boolean doAuth(HttpRequestImpl httpRequest, HandlerManager.HandlerEntry handlerEntry) {
        String host = httpRequest.getRemoteHost();
        AuthFailure failure = authFailures.computeIfAbsent(host, k -> new AuthFailure());
        if (failure.isLocked(maxAuthFailures, authFailWindowMillis)) return false;

        boolean doneAuth = authenticateWithHandler(httpRequest, handlerEntry);

        if (doneAuth) {
            authFailures.remove(host); // 认证成功后清空计数
        } else {
            failure.record();
            evictOverflow(); // 仅失败路径才可能超限，成功请求不必付遍历代价
        }

        return doneAuth;
    }

    // 超限后先回收窗口过期项再按最久未失败淘汰：只淘汰不回收，窗口内的新来源会挤掉有效记录，防爆破失效
    private void evictOverflow() {
        if (authFailures.size() <= MAX_TRACKED_HOSTS) return; // 容量未超，不做任何遍历
        long expireBefore = System.currentTimeMillis() - authFailWindowMillis;
        authFailures.entrySet().removeIf(e -> e.getValue().lastFailureAt() < expireBefore);

        while (authFailures.size() > MAX_TRACKED_HOSTS) {
            String oldest = authFailures.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastFailureAt()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest == null) return;
            authFailures.remove(oldest);
        }
    }

    private boolean authenticateWithHandler(HttpRequestImpl httpRequest, HandlerManager.HandlerEntry handlerEntry) {
        AuthResult authResult = null;
        boolean needAuth = !handlerEntry.noAuth; // 认证只能按路径由 noAuth 豁免，无全局开关
        if (needAuth) {
            authResult = authenticate(httpRequest);
        }

        // 不需要校验
        if (authResult == null) return true;

        // 校验未通过
        if (authResult.status() != AuthResult.Status.PASS) return false;

        // PASS 但无主体是模糊态：角色无从归属，handler 按角色鉴权只会拿到 null，故按未认证拒绝
        if (authResult.getPrincipal() == null) return false;

        // 校验通过：写入受保护的认证字段而非通用 attribute，避免被 handler 覆写
        httpRequest.setAuth(authResult);
        return true;
    }

    private AuthResult authenticate(HttpRequest request) {
        if (authenticators.isEmpty()) return AuthResult.reject("no authenticator ready");

        AuthResult reject = null;
        for (Authenticator authenticator : authenticators) {
            AuthResult r;
            try {
                r = authenticator.authenticate(request);
            } catch (Throwable e) {
                logger.error("authentication error: " + authenticator.getClass().getName(), e);
                r = AuthResult.reject("authentication error"); // 单个认证器故障转为拒绝，不影响其余认证器继续判定
            }
            if (r == null || r.status() == AuthResult.Status.ABSTAIN) continue; // 弃权：转交后续认证器
            if (r.status() == AuthResult.Status.PASS) return r;

            if (reject == null) reject = r;
        }
        if (reject != null) return reject;

        return AuthResult.reject("no credential provided");
    }

    // 用实例自身锁即可：本类实例不逃逸出 AuthManager，三个方法守的是同一组状态
    static final class AuthFailure {
        private int count;
        private long windowStart = System.currentTimeMillis();
        private long lastFailureAt = windowStart;

        synchronized void record() {
            count++;
            lastFailureAt = System.currentTimeMillis();
        }

        // count 与 windowStart 须在同一锁内判定：否则并行喷射时窗口重置会擦掉已累计的失败数，节流失效
        synchronized boolean isLocked(int maxFailures, int windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                windowStart = now;
                count = 0;
            }
            return count > 0 && count >= maxFailures;
        }

        // 非 volatile 的 long 不保证原子读（JLS 17.7），须在锁内读取
        synchronized long lastFailureAt() {
            return lastFailureAt;
        }
    }
}
