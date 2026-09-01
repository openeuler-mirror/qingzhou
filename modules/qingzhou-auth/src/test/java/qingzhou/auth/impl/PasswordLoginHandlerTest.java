package qingzhou.auth.impl;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.auth.TokenService;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;

public class PasswordLoginHandlerTest {

    @Test
    public void validCredentials_login_returnsToken() throws Exception {
        StubHttpResponse response = new StubHttpResponse();
        StubTokenService tokenService = new StubTokenService();
        PasswordLoginHandler handler = buildHandler("secret", 5, 300, tokenService);
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "secret"), response);

        Assert.assertEquals(response.status, 200);
        Assert.assertEquals(response.body, "{\"token\":\"stub-token\"}");
        Assert.assertEquals(tokenService.user, "admin"); // 登录成功以用户名签发 token
    }

    @Test
    public void invalidPassword_login_returns401() throws Exception {
        StubHttpResponse response = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 5, 300, new StubTokenService());
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "wrong"), response);

        Assert.assertEquals(response.status, 401);
        Assert.assertEquals(response.body, "invalid user or password");
    }

    @Test
    public void getMethod_login_returns405() throws Exception {
        StubHttpResponse response = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 5, 300, new StubTokenService());
        handler.handle(request("/auth/login", "GET", "127.0.0.1", "admin", "secret"), response);

        Assert.assertEquals(response.status, 405);
        Assert.assertEquals(response.body, "method not allowed");
    }

    @Test
    public void unknownPath_request_returns400() throws Exception {
        StubHttpResponse response = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 5, 300, new StubTokenService());
        handler.handle(request("/auth/other", "POST", "127.0.0.1", "admin", "secret"), response);

        Assert.assertEquals(response.status, 400);
    }

    @Test
    public void logoutPath_request_returns200() throws Exception {
        StubHttpResponse response = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 5, 300, new StubTokenService());
        handler.handle(request("/auth/logout", "POST", "127.0.0.1", null, null), response);

        Assert.assertEquals(response.status, 200);
        Assert.assertEquals(response.body, "ok");
    }

    @Test
    public void digestPassword_login_verifiesDigest() throws Exception {
        CryptoImpl crypto = new CryptoImpl();
        String digest = crypto.getMessageDigest().digest("secret", "SHA-256", 16, 2);
        StubHttpResponse response = new StubHttpResponse();
        PasswordLoginHandler handler = new PasswordLoginHandler();
        setField(handler, "crypto", crypto);
        setField(handler, "tokenService", new StubTokenService());
        Map<String, String> config = new HashMap<>();
        config.put("username", "admin");
        config.put("password", digest); // 直接提供摘要格式（alg$salt$iterations$digest）
        config.put("max_failures", "5");
        config.put("lock_seconds", "300");
        handler.start(config);
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "secret"), response);

        Assert.assertEquals(response.status, 200);
    }

    @Test
    public void tooManyFailures_login_returns429() throws Exception {
        StubHttpResponse first = new StubHttpResponse();
        StubHttpResponse second = new StubHttpResponse();
        StubHttpResponse third = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 2, 300, new StubTokenService()); // 失败 2 次后锁定
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "wrong"), first);
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "wrong"), second);
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "secret"), third);

        Assert.assertEquals(first.status, 401);
        Assert.assertEquals(second.status, 401);
        Assert.assertEquals(third.status, 429);
        Assert.assertEquals(third.body, "too many login failures, try again later");
    }

    @Test
    public void lockWindowExpired_login_succeedsAgain() throws Exception {
        StubHttpResponse firstFailure = new StubHttpResponse();
        StubHttpResponse locked = new StubHttpResponse();
        StubHttpResponse unlocked = new StubHttpResponse();
        PasswordLoginHandler handler = buildHandler("secret", 1, 1, new StubTokenService()); // 失败 1 次即锁定，1 秒后解锁
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "wrong"), firstFailure);
        Assert.assertEquals(firstFailure.status, 401); // 第 1 次失败仅 401

        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "secret"), locked);
        Assert.assertEquals(locked.status, 429); // 达阈值后锁定

        Thread.sleep(1100); // 等待锁定窗口过期
        handler.handle(request("/auth/login", "POST", "127.0.0.1", "admin", "secret"), unlocked);

        Assert.assertEquals(unlocked.status, 200);
    }

    @Test
    public void anyRequest_excludedPaths_returnsLoginEndpoints() {
        Assert.assertEquals(PasswordLoginHandler.EXCLUDED_PATHS,
                new String[]{"/auth/login", "/auth/logout"});
    }

    // ---------- 辅助 ----------

    private PasswordLoginHandler buildHandler(String plainPassword, int maxFailures, int lockSeconds, TokenService tokenService) throws Exception {
        PasswordLoginHandler handler = new PasswordLoginHandler();
        CryptoImpl crypto = new CryptoImpl();
        setField(handler, "crypto", crypto);
        setField(handler, "tokenService", tokenService);

        Map<String, String> config = new HashMap<>();
        config.put("username", "admin");
        config.put("password", crypto.getMessageDigest().digest(plainPassword, "SHA-256", 16, 2)); // 新实现仅接受摘要格式
        config.put("max_failures", String.valueOf(maxFailures));
        config.put("lock_seconds", String.valueOf(lockSeconds));
        handler.start(config);
        return handler;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private StubHttpRequest request(String path, String method, String ip, String user, String password) {
        StubHttpRequest request = new StubHttpRequest();
        request.path = path;
        request.method = method;
        request.remoteHost = ip;
        request.params.put("user", user);
        request.params.put("password", password);
        return request;
    }

    static class StubTokenService implements TokenService {
        String user;

        @Override
        public String createToken(String user) {
            this.user = user;
            return "stub-token";
        }

        @Override
        public String verifyToken(String token) {
            return null;
        }
    }

    static class StubHttpRequest implements HttpRequest {
        String path;
        String method;
        String remoteHost;
        String header;
        final Map<String, String> params = new HashMap<>();

        @Override
        public String getRemoteHost() {
            return remoteHost;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getFullPath() {
            return path;
        }

        @Override
        public String getParameter(String name) {
            return params.get(name);
        }

        @Override
        public Map<String, List<String>> getParameters() {
            return null;
        }

        @Override
        public String getHeader(String header) {
            return this.header;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public boolean isFormUrlencoded() {
            return false;
        }

        @Override
        public byte[] getBody() {
            return new byte[0];
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }
    }

    static class StubHttpResponse implements HttpResponse {
        int status = 200; // HTTP 默认成功状态，显式设置时覆盖
        String body;

        @Override
        public void status500Finish(String msg) {
            this.status = 500;
            this.body = msg;
        }

        @Override
        public void status404Finish() {
            this.status = 404;
        }

        @Override
        public void status400Finish() {
            this.status = 400;
        }

        @Override
        public HttpResponse status(int status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponse header(String name, String value) {
            return this;
        }

        @Override
        public HttpResponse contentType(String value) {
            return this;
        }

        @Override
        public HttpResponse contentTypeJsonUtf8() {
            return this;
        }

        @Override
        public HttpResponse send(String bodyAsUtf8) {
            this.body = bodyAsUtf8;
            return this;
        }

        @Override
        public HttpResponse send(byte[] body) {
            this.body = new String(body, StandardCharsets.UTF_8);
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public void sendFinish(String bodyAsUtf8) {
            this.body = bodyAsUtf8;
        }

        @Override
        public void sendFinish(byte[] body) {
            this.body = new String(body, StandardCharsets.UTF_8);
        }
    }
}
