package qingzhou.monitor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;

@Component(configurationPid = "qingzhou-monitor", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = CustomAuthenticator.class)
public class CustomAuthenticator { // 不要 implements Authenticator，否则会被识别为系统级认证器
    @Reference
    private Crypto crypto;

    private String authToken;
    private String authTokenParamName;
    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        authToken = config.getOrDefault("token", "").trim();
        authTokenParamName = config.getOrDefault("token_param_name", "").trim();

        tokenCipher = crypto.getGlobalCipher();
    }

    AuthResult authenticate(HttpRequest request) {
        if (authToken.isEmpty() || authTokenParamName.isEmpty()) return null; // null 会转交给系统级认证

        String requestToken = request.getParameter(authTokenParamName);
        if (requestToken == null) {
            return AuthResult.reject("token missing");
        }

        String decrypt = tokenCipher.tryDecrypt(authToken, "qingzhou-monitor.token");
        if (MessageDigest.isEqual(requestToken.getBytes(StandardCharsets.UTF_8), decrypt.getBytes(StandardCharsets.UTF_8))) {
            return AuthResult.pass(null, null);
        }

        return AuthResult.reject("invalid token");
    }
}
