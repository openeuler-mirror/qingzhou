package qingzhou.http.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.*;
import qingzhou.logger.Logger;

import static qingzhou.http.impl.HttpServerImpl.getConfig;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    @Reference
    private Logger logger;

    private final List<String> tempMsg = new ArrayList<>();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    // 以下由 OSGi 激活线程写入、Netty EventLoop 读取，须保证可见性
    private volatile boolean isAuthDisabled;

    private volatile int maxAuthFailures; // 窗口内允许的认证失败次数
    private volatile int authFailWindowMillis;
    private final Map<String, AuthFailure> authFailures = new ConcurrentHashMap<>();

    @Activate
    public synchronized void init(Map<String, String> config) { // 与 addAuthenticator 同锁：tempMsg 的暂存-回放与动态绑定并发互斥
        isAuthDisabled = getConfig(config, "auth_disabled", false);
        if (isAuthDisabled) logger.warn("http server authentication is disabled");

        maxAuthFailures = getConfig(config, "auth_fail_max", 10);
        authFailWindowMillis = getConfig(config, "auth_fail_window", 60) * 1000;

        tempMsg.forEach(s -> logger.info(s));
        tempMsg.clear();
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeAuthenticator")
    public synchronized void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);

        // 在 ReferencePolicy.DYNAMIC 内，Logger 可能尚未注入，故先暂存消息，在 @Activate 中一起输出
        String msg = "http authenticator registered: " + authenticator.getClass().getName();
        if (logger != null) {
            logger.info(msg);
        } else {
            tempMsg.add(msg);
        }
    }

    public void removeAuthenticator(Authenticator authenticator) {
        authenticators.remove(authenticator);
    }

    /**
     * 安全认证：多认证器聚合——任一 PASS 即放行；首个显式 REJECT 优先拒绝（客户端已出示凭据，须明确告知 401）；
     * 全部既未 PASS 也未 REJECT 时按无凭据拒绝。
     * 免认证标记取自 handlerEntry（随注册路径），全局开关 auth_disabled 在此处判断。
     */
    boolean doAuth(HttpRequestImpl httpRequest, HandlerManager.HandlerEntry handlerEntry) {
        String host = httpRequest.getRemoteHost();
        AuthFailure failure = authFailures.computeIfAbsent(host, k -> new AuthFailure());
        if (failure.isLocked(maxAuthFailures, authFailWindowMillis)) return false;

        boolean doneAuth = doAuth0(httpRequest, handlerEntry);

        if (doneAuth) {
            authFailures.remove(host); // 认证成功后清空计数
        } else {
            failure.record();
            evictOverflow(); // 仅失败路径才可能超限，成功请求不必付遍历代价
        }

        return doneAuth;
    }

    // 超限后按最久未失败淘汰：只回收过期项的话，窗口内的新来源永远删不掉，map 会无界增长
    private void evictOverflow() {
        int maxTrackedHosts = 10000; // 来源数量上限，超限时淘汰最久未失败者
        while (authFailures.size() > maxTrackedHosts) {
            String oldest = authFailures.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastFailureAt))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest == null) return;
            authFailures.remove(oldest);
        }
    }

    private boolean doAuth0(HttpRequestImpl httpRequest, HandlerManager.HandlerEntry handlerEntry) {
        HttpHandler httpHandler = handlerEntry.handler;

        AuthResult authResult = null;
        // 自定义 Authenticator，优先使用
        HandlerAuthenticator customAuthenticator = httpHandler.customAuthenticator();
        if (customAuthenticator != null) {
            AuthResult custom;
            try {
                custom = customAuthenticator.authenticate(httpRequest);
            } catch (Throwable e) { // 认证器出错一律拒绝：不放行，且留下可排障的日志而非静默断连
                // 必须是 Throwable：OSGi 刷新/卸载 bundle 时会抛 NoClassDefFoundError，只捕 Exception 会静默穿透
                logger.error("custom authentication error: " + httpHandler.getClass().getName(), e);
                return false;
            }
            if (custom != null && custom.status() != AuthResult.Status.ABSTAIN) authResult = custom;
        }
        // 系统级 Authenticator
        if (authResult == null) {
            boolean needAuth = !isAuthDisabled && !handlerEntry.noAuth;
            if (needAuth) {
                authResult = authenticate(httpRequest);
            }
        }

        // 不需要校验
        if (authResult == null) return true;

        // 校验未通过
        if (authResult.status() != AuthResult.Status.PASS) return false;

        // 校验通过
        if (authResult.getPrincipal() != null) {
            httpRequest.setAttribute(AuthResult.AUTH_PRINCIPAL_ATTRIBUTE, authResult.getPrincipal());
        }
        if (authResult.getRoles() != null) {
            httpRequest.setAttribute(AuthResult.AUTH_ROLES_ATTRIBUTE, authResult.getRoles());
        }
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

    static final class AuthFailure {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long windowStart = System.currentTimeMillis();
        private long lastFailureAt = windowStart;

        void record() {
            count.incrementAndGet();
            lastFailureAt = System.currentTimeMillis();
        }

        // count 与 windowStart 须在同一锁内读写：否则并行喷射时窗口重置会擦掉已累计的失败数，节流失效
        boolean isLocked(int maxFailures, int windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                windowStart = now;
                count.set(0);
            }
            int failures = count.get();
            return failures > 0 && failures >= maxFailures;
        }
    }
}
