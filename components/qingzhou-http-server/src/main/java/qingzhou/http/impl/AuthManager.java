package qingzhou.http.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.*;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.Authenticator;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;

@Component(service = AuthManager.class)
public class AuthManager {
    @Reference
    private Logger logger;

    private final List<String> tempMsg = new ArrayList<>();

    // OSGi 动态绑定与请求线程并发读写，须用写时复制容器避免遍历中结构变更
    private final List<Authenticator> authenticators = new CopyOnWriteArrayList<>();

    @Activate
    public void start() {
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

    public synchronized void removeAuthenticator(Authenticator authenticator) {
        authenticators.remove(authenticator);
    }


    /**
     * 安全认证：配置 auth_disabled=true 时全局关闭；多认证器按 pass > reject > challenge > missing 组合——
     * 任一通过即放行；凭据无效优先拒绝（客户端已出示凭据，须明确告知 401 而非重定向）；
     * 全部无凭据时才用重定向引导登录。
     */
    AuthResult authenticate(HttpRequest request) {
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
