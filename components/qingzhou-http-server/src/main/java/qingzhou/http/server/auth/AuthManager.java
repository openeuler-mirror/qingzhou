package qingzhou.http.server.auth;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.Configs;
import qingzhou.common.CounterLockout;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.http.server.impl.HandlerManager;
import qingzhou.http.server.impl.HttpRequestImpl;

@Component(configurationPid = "qingzhou-http-server", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AuthManager.class)
public class AuthManager {
    @Reference
    private TokenService tokenService;

    private CounterLockout lockout;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        int maxAuthFailures = configs.atLeast("auth_fail_max", 1, 10);
        long authFailWindowSeconds = configs.between("auth_fail_window", 1L, 86400L, 60);
        long authFailWindowMillis = authFailWindowSeconds * 1000L;
        lockout = new CounterLockout(maxAuthFailures, authFailWindowMillis, 10_000);
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

        // 仅当请求确实携带了凭据却失败（REJECT）才计入锁定并告警；
        // 未出示凭据（NO_CREDENTIAL）的匿名访问（NAT 下大量正常请求）不应消耗锁定计数，也不应逐条刷日志，否则会误锁并淹没同 IP 下的全部正常用户
        if (authResult.status() != AuthResult.Status.NO_CREDENTIAL) {
            lockout.record(host);
        }
        return false;
    }

    private AuthResult authenticate(HttpRequest request, HttpResponse response) {
        String token = tokenService.retrieveToken(request);
        if (token == null) return AuthResult.noCredential(); // 未出示凭据：不计入爆破锁定

        Object[] userRoles = tokenService.verifyToken(token, request, response);
        if (userRoles == null) return AuthResult.reject();

        return AuthResult.pass((String) userRoles[0], (String[]) userRoles[1]);
    }
}
