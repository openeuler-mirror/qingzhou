package qingzhou.ai.impl;

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

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = McpAuthenticator.class)
public class McpAuthenticator { // 不要 implements Authenticator，否则会被识别为系统级认证器
    @Reference
    private Crypto crypto;

    private String authToken;
    private String authTokenParamName;
    private Cipher tokenCipher;

    @Activate
    public void init(Map<String, String> config) {
        authToken = config.getOrDefault("mcp_auth_token", "").trim();

        tokenCipher = crypto.getGlobalCipher();
    }

    AuthResult authenticate(HttpRequest request) {
        if (authToken.isEmpty()) return AuthResult.abstain(); // 未配置令牌：转交系统级认证

        String requestToken = bearer(request);
        if (requestToken == null) {
            return AuthResult.reject("token missing");
        }

        String decrypt = tokenCipher.tryDecrypt(authToken, "qingzhou-ai.mcp_auth_token");
        if (MessageDigest.isEqual(requestToken.getBytes(StandardCharsets.UTF_8), decrypt.getBytes(StandardCharsets.UTF_8))) {
            return AuthResult.pass(null, null);
        }

        return AuthResult.reject("invalid token");
    }

    private static String bearer(HttpRequest request) {
        String BEARER = "Bearer ";
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) return null;
        return header.substring(BEARER.length()).trim();
    }
}
