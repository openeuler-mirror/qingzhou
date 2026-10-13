package qingzhou.http.server.auth.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.common.FailureLockout;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.auth.AuthResult;
import qingzhou.http.server.auth.Authenticator;
import qingzhou.http.server.impl.HandlerManager;
import qingzhou.http.server.impl.HttpRequestImpl;
import qingzhou.logger.Logger;
import qingzhou.logger.util.PendingLogs;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    @Reference
    private Logger logger;

    private final PendingLogs pendingLogs = new PendingLogs();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    private volatile FailureLockout lockout;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        int maxAuthFailures = configs.atLeast("auth_fail_max", 1, 10);
        long authFailWindowSeconds = configs.between("auth_fail_window", 1L, 86400L, 60);
        long authFailWindowMillis = authFailWindowSeconds * 1000L;
        lockout = new FailureLockout(maxAuthFailures, authFailWindowMillis, 10_000);

        pendingLogs.replay(logger);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            unbind = "removeAuthenticator")
    public void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);

        pendingLogs.info(logger, "http authenticator registered: " + authenticator.getClass().getName());
    }

    public void removeAuthenticator(Authenticator authenticator) {
        authenticators.remove(authenticator);
    }

    /**
     * 安全认证：多认证器聚合——任一 PASS 即放行；首个显式 REJECT 优先拒绝（客户端已出示凭据，须明确告知 401）；
     * 全部既未 PASS 也未 REJECT 时按无凭据拒绝。
     * 免认证标记取自 handlerEntry（随注册路径）；不提供全局免认证开关，避免一处配置放通全站。
     *
     * @return null 表示放行（免认证或认证通过，PASS 结果可携带续期 Set-Cookie）；否则为拒绝结果
     */
    public AuthResult doAuth(HttpRequestImpl httpRequest, HandlerManager.HandlerEntry handlerEntry) {
        if (handlerEntry.noAuth) return null;

        String host = httpRequest.getRemoteHost();
        if (lockout.isLocked(host)) return AuthResult.reject("auth lockout");

        AuthResult authResult = authenticate(httpRequest);

        // 校验通过：清空失败计数并写入受保护的认证字段（避免被 handler 覆写）
        if (authResult.status() == AuthResult.Status.PASS && authResult.getPrincipal() != null) {
            lockout.clear(host);
            httpRequest.setPrincipal(authResult.getPrincipal(), authResult.getRoles());
            return authResult;
        }

        // 仅当请求确实携带了凭据却失败才计入锁定：未携带凭据的匿名访问（NAT 下大量正常请求）
        // 不应消耗锁定计数，否则会误锁同 IP 下的全部正常用户。判断依据是认证结果原因而非直接读头：
        // TokenAuthenticator 对无/畸形 Bearer 返回 "token missing"，即"未出示凭据"，不计入锁定；
        // 只有出示了凭据却失败（如 "invalid token"）才消耗锁定计数，从而仍能拖慢凭据爆破
        String reason = authResult.getReason();
        boolean noCredential = reason == null || "no credential provided".equals(reason)
                || "no authenticator ready".equals(reason)
                || "token missing".equals(reason);
        if (!noCredential) {
            lockout.recordFailure(host);
        }

        logger.warn("http authentication rejected: " + reason);
        return authResult.status() == AuthResult.Status.REJECT ? authResult
                : AuthResult.reject("no credential provided"); // PASS 而无主体属模糊态：按无凭据拒绝
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
            if (r == null || r.status() == null) continue; // 弃权或无效结果：转交后续认证器，不得让单个认证器阻断其余
            switch (r.status()) {
                case PASS:
                    if (r.getPrincipal() == null) break; // PASS 但无主体属模糊态：不采信，交由后续认证器判定
                    return r;
                case REJECT:
                    if (reject == null) reject = r;
                    break;
            }
        }
        if (reject != null) return reject;

        return AuthResult.reject("no credential provided");
    }
}
