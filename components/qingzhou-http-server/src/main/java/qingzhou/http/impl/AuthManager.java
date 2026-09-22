package qingzhou.http.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;

import static qingzhou.http.impl.HttpServerImpl.getConfig;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    @Reference
    private Logger logger;
    @Reference
    private HandlerManager handlerManager;

    private final List<String> tempMsg = new ArrayList<>();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    private boolean isAuthDisabled;

    @Activate
    public synchronized void init(Map<String, String> config) { // 与 addAuthenticator 同锁：tempMsg 的暂存-回放与动态绑定并发互斥
        isAuthDisabled = getConfig(config, "auth_disabled", false);
        if (isAuthDisabled) logger.warn("http server authentication is disabled");

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
        HttpHandler httpHandler = handlerEntry.handler;

        AuthResult authResult = null;
        // 自定义 Authenticator，优先使用
        Authenticator customAuthenticator = httpHandler.customAuthenticator();
        if (customAuthenticator != null) {
            try {
                authResult = customAuthenticator.authenticate(httpRequest);
            } catch (Exception e) { // 认证器出错一律拒绝：不放行，且留下可排障的日志而非静默断连
                logger.error("custom authentication error: " + httpHandler.getClass().getName(), e);
                return false;
            }
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
            } catch (Exception e) {
                logger.error("authentication error: " + authenticator.getClass().getName(), e);
                r = AuthResult.reject("authentication error");
            }
            if (r.status() == AuthResult.Status.PASS) return r;

            if (r.status() == AuthResult.Status.REJECT && reject == null) {
                reject = r;
            }
        }
        if (reject != null) return reject;

        return AuthResult.reject("no credential provided");
    }
}
