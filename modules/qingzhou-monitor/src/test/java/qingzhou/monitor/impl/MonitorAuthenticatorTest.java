package qingzhou.monitor.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;

/**
 * 令牌校验自动化测试集：Authorization 头优先，query 传参仅在显式配置时兼容，未配置令牌则弃权。
 */
public class MonitorAuthenticatorTest {
    private static final String TOKEN = "secret-token";

    @Test
    public void bearerHeaderWithValidToken_authenticate_passes() throws Exception {
        MonitorAuthenticator authenticator = authenticator(true, true);

        Assert.assertSame(authenticator.authenticate(request("Bearer " + TOKEN, null)).status(),
                AuthResult.Status.PASS);
    }

    @Test
    public void queryParamWithValidToken_authenticate_passes() throws Exception {
        MonitorAuthenticator authenticator = authenticator(true, true);

        Assert.assertSame(authenticator.authenticate(request(null, TOKEN)).status(),
                AuthResult.Status.REJECT);
    }

    @Test
    public void noCredential_authenticate_rejects() throws Exception {
        MonitorAuthenticator authenticator = authenticator(true, true);

        Assert.assertSame(authenticator.authenticate(request(null, null)).status(),
                AuthResult.Status.REJECT);
    }

    @Test
    public void wrongToken_authenticate_rejects() throws Exception {
        MonitorAuthenticator authenticator = authenticator(true, true);

        Assert.assertSame(authenticator.authenticate(request("Bearer wrong", null)).status(),
                AuthResult.Status.REJECT);
    }

    @Test
    public void tokenNotConfigured_authenticate_abstains() throws Exception {
        MonitorAuthenticator authenticator = authenticator(false, false);

        Assert.assertSame(authenticator.authenticate(request("Bearer " + TOKEN, TOKEN)).status(),
                AuthResult.Status.ABSTAIN);
    }

    @Test
    public void queryParamNotConfigured_queryToken_authenticate_rejects() throws Exception {
        MonitorAuthenticator authenticator = authenticator(true, false);

        Assert.assertSame(authenticator.authenticate(request(null, TOKEN)).status(),
                AuthResult.Status.REJECT);
    }

    private static MonitorAuthenticator authenticator(boolean withToken, boolean withParamName) throws Exception {
        MonitorAuthenticator authenticator = new MonitorAuthenticator();
        Field crypto = MonitorAuthenticator.class.getDeclaredField("crypto");
        crypto.setAccessible(true);
        crypto.set(authenticator, new CryptoImpl());

        Map<String, String> config = new HashMap<>();
        if (withToken) config.put("auth_token", Cipher.PLAIN_PREFIX_MARKER + TOKEN);
        authenticator.init(config);
        return authenticator;
    }

    private static HttpRequest request(String authorization, String queryToken) {
        return (HttpRequest) Proxy.newProxyInstance(HttpRequest.class.getClassLoader(),
                new Class<?>[]{HttpRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeader":
                            return authorization;
                        case "getParameter":
                            return queryToken;
                        default:
                            return null;
                    }
                });
    }
}
