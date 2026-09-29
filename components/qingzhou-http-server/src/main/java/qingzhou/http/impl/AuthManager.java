package qingzhou.http.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;

import static qingzhou.http.impl.ConfigUtil.*;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    @Reference
    private Logger logger;

    private final PendingLogs pendingLogs = new PendingLogs();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性
    private volatile int maxAuthFailures; // 窗口内允许的认证失败次数
    private volatile long authFailWindowMillis;
    private final Map<String, AuthFailure> authFailures = new ConcurrentHashMap<>();

    @Activate
    public void init(Map<String, String> config) { // 暂存日志的回放与动态绑定的互斥由 PendingLogs 自身同步保证
        // 窗口与次数同样须校验：窗口非正数会让过期判定恒真，计数每次请求都被清零，防爆破静默失效；
        // 上界防 *1000 溢出为负（同样令过期判定恒真），且超过一天的锁定窗口本就无意义
        maxAuthFailures = atLeast(getConfig(config, "auth_fail_max", 10), 1, "auth_fail_max");
        int authFailWindowSeconds = atMost(
                atLeast(getConfig(config, "auth_fail_window", 60), 1, "auth_fail_window"),
                86400, "auth_fail_window");
        authFailWindowMillis = authFailWindowSeconds * 1000L;

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
        if (failure.isLocked(maxAuthFailures, authFailWindowMillis)) {
            // 爆破行为必须留痕，否则只能在日志里看到一堆 401 而无从定位来源
            if (failure.markLockNotified()) {
                logger.warn("auth locked: [" + host + "] exceeded " + maxAuthFailures
                        + " failures within " + authFailWindowMillis / 1000 + "s");
            }
            return false;
        }

        boolean doneAuth = authenticateWithHandler(httpRequest, handlerEntry);

        if (doneAuth) {
            authFailures.remove(host); // 认证成功后清空计数
        } else {
            failure.record();
            evictOverflow(); // 仅失败路径才可能超限，成功请求不必付遍历代价
        }

        return doneAuth;
    }

    // 超限后先回收窗口过期项再按最久未失败淘汰到低水位：只淘汰不回收，窗口内的新来源会挤掉有效记录，防爆破失效
    private void evictOverflow() {
        int MAX_TRACKED_HOSTS = 10000; // 来源数量上限：达到即触发淘汰
        int EVICT_WATERMARK = MAX_TRACKED_HOSTS * 9 / 10; // 淘汰低水位：一次淘汰到上限的 90%，把 O(n) 全表遍历摊销到每新增约 10% 来源一次

        if (authFailures.size() <= MAX_TRACKED_HOSTS) return; // 未达上限，不做任何遍历
        // 先摘除窗口过期项：它们在限流意义上已自动失效（isLocked 到期即重置），
        // 但若留给堆淘汰，堆积的过期项会挤占名额、连累窗口内活跃来源的计数被误删
        long expireBefore = System.currentTimeMillis() - authFailWindowMillis;
        authFailures.entrySet().removeIf(e -> e.getValue().lastFailureAt() < expireBefore);

        // 一次淘汰到低水位：摊销全表遍历，避免容量超限期间每个失败请求都付一次 O(n)
        int excess = authFailures.size() - EVICT_WATERMARK;
        if (excess <= 0) return;
        // 有界堆一次遍历选出最久未失败的 excess 个：堆序取倒序（堆顶为最近失败），
        // 溢出时弹出的是较新者，留下的才是最久未失败者
        PriorityQueue<Map.Entry<String, AuthFailure>> oldest = new PriorityQueue<>(excess + 1,
                Comparator.<Map.Entry<String, AuthFailure>>comparingLong(e -> e.getValue().lastFailureAt()).reversed());
        for (Map.Entry<String, AuthFailure> entry : authFailures.entrySet()) {
            oldest.offer(entry);
            if (oldest.size() > excess) oldest.poll();
        }
        for (Map.Entry<String, AuthFailure> entry : oldest) authFailures.remove(entry.getKey());
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
        private boolean lockNotified;
        private long windowStart = System.currentTimeMillis();
        private long lastFailureAt = windowStart;

        synchronized void record() {
            count++;
            lastFailureAt = System.currentTimeMillis();
        }

        // count 与 windowStart 须在同一锁内判定：否则并行喷射时窗口重置会擦掉已累计的失败数，节流失效
        synchronized boolean isLocked(int maxFailures, long windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                windowStart = now;
                count = 0;
                lockNotified = false; // 窗口已过期，再次锁定须重新告警
            }
            return count >= maxFailures; // maxFailures 已校验为正，count 为 0 时不可能命中
        }

        // 锁定期间每次请求都会走到这里，只记首次：逐条记会被爆破流量本身刷成日志洪水
        synchronized boolean markLockNotified() {
            boolean first = !lockNotified;
            lockNotified = true;
            return first;
        }

        // 非 volatile 的 long 不保证原子读（JLS 17.7），须在锁内读取
        synchronized long lastFailureAt() {
            return lastFailureAt;
        }
    }
}
