package qingzhou.http.server.auth.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.common.Configs;
import qingzhou.common.FailureLockout;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
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
     */
    public boolean doAuth(HttpRequestImpl httpRequest, HttpResponse httpResponse, HandlerManager.HandlerEntry handlerEntry) {
        if (handlerEntry.noAuth) return true;

        String host = httpRequest.getRemoteHost();
        if (lockout.isLocked(host)) return false;

        AuthResult authResult = authenticate(httpRequest, httpResponse);

        // 校验通过：清空失败计数并写入受保护的认证字段（避免被 handler 覆写）
        if (authResult.status() == AuthResult.Status.PASS && authResult.getPrincipal() != null) {
            lockout.clear(host);
            httpRequest.setPrincipal(authResult.getPrincipal(), authResult.getRoles());
            return true;
        }

        // 仅当请求确实携带了凭据却失败（REJECT）才计入锁定；
        // 未出示凭据（NO_CREDENTIAL）的匿名访问（NAT 下大量正常请求）不应消耗锁定计数，否则会误锁同 IP 下的全部正常用户；
        if (authResult.status() != AuthResult.Status.NO_CREDENTIAL) {
            lockout.recordFailure(host);
        }

        logger.warn("http authentication rejected: " + sanitizeLogText(authResult.getReason()));
        return false;
    }

    // 认证器可能把用户输入放进 reason，剔除换行防止日志伪造
    private static String sanitizeLogText(String text) {
        return text == null ? "" : text.replace("\r", "\\r").replace("\n", "\\n");
    }

    private AuthResult authenticate(HttpRequest request, HttpResponse response) {
        if (authenticators.isEmpty()) return AuthResult.noCredential("no authenticator ready");

        AuthResult reject = null;
        for (Authenticator authenticator : authenticators) {
            AuthResult r;
            try {
                r = authenticator.authenticate(request, response);
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

        return AuthResult.noCredential("no credential provided");
    }
}
