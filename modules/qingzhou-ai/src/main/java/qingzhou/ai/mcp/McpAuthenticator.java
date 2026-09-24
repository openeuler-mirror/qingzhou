package qingzhou.ai.mcp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HandlerAuthenticator;
import qingzhou.http.server.HttpRequest;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = McpAuthenticator.class)
public class McpAuthenticator implements HandlerAuthenticator { // 系统级认证器是 Authenticator，两者刻意不同型
    @Reference
    private Crypto crypto;

    // OSGi 激活线程写入，Netty EventLoop 读取，须保证可见性
    private volatile String authToken;

    @Activate
    public void init(Map<String, String> config) {
        String raw = config.getOrDefault("mcp_auth_token", "").trim();
        // 解密只在激活时做一次：每请求解密会在明文配置下反复打印告警并放大 I/O
        authToken = raw.isEmpty() ? "" : crypto.getGlobalCipher().tryDecrypt(raw, "qingzhou-ai.mcp_auth_token");
    }

    @Override
    public AuthResult authenticate(HttpRequest request) {
        return verify(request, authToken); // 未配置令牌时返回 abstain，转交系统级认证
    }

    private AuthResult verify(HttpRequest request, String expected) {
        if (expected == null || expected.isEmpty()) return AuthResult.abstain();

        String prefix = "Bearer ";
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(prefix)) return AuthResult.reject("token missing");
        String token = header.substring(prefix.length()).trim();

        // 常量时间比较：逐字节提前退出会按响应耗时泄露令牌前缀
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))
                ? AuthResult.pass(null, null)
                : AuthResult.reject("invalid token");
    }
}
