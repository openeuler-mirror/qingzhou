package qingzhou.auth;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.server.AuthResult;

public class TokenAuthenticatorTest {

    @Test
    public void noAuthorizationHeader_authenticate_returnsMissing() throws Exception {
        initTokenCipher();
        PasswordLoginTest.StubHttpRequest request = new PasswordLoginTest.StubHttpRequest();

        AuthResult result = new TokenAuthenticator().authenticate(request);

        Assert.assertTrue(result.isMissing());
    }

    @Test
    public void nonBearerHeader_authenticate_returnsMissing() throws Exception {
        initTokenCipher();
        PasswordLoginTest.StubHttpRequest request = new PasswordLoginTest.StubHttpRequest();
        request.header = "Basic abc";

        AuthResult result = new TokenAuthenticator().authenticate(request);

        Assert.assertTrue(result.isMissing());
    }

    @Test
    public void validToken_authenticate_returnsPass() throws Exception {
        initTokenCipher();
        String token = PasswordLogin.tokenCipher.encrypt("admin|" + (System.currentTimeMillis() + 60_000));
        PasswordLoginTest.StubHttpRequest request = new PasswordLoginTest.StubHttpRequest();
        request.header = "Bearer " + token;

        AuthResult result = new TokenAuthenticator().authenticate(request);

        Assert.assertTrue(result.isPassed());
        Assert.assertEquals(result.getPrincipal(), "admin");
    }

    @Test
    public void invalidToken_authenticate_returnsReject() throws Exception {
        initTokenCipher();
        PasswordLoginTest.StubHttpRequest request = new PasswordLoginTest.StubHttpRequest();
        request.header = "Bearer not-a-valid-token";

        AuthResult result = new TokenAuthenticator().authenticate(request);

        Assert.assertTrue(result.isRejected());
        Assert.assertEquals(result.getReason(), "invalid token");
    }

    @Test
    public void expiredToken_authenticate_returnsReject() throws Exception {
        initTokenCipher();
        String token = PasswordLogin.tokenCipher.encrypt("admin|" + (System.currentTimeMillis() - 1000));
        PasswordLoginTest.StubHttpRequest request = new PasswordLoginTest.StubHttpRequest();
        request.header = "Bearer " + token;

        AuthResult result = new TokenAuthenticator().authenticate(request);

        Assert.assertTrue(result.isRejected());
    }

    @Test
    public void anyRequest_excludedPaths_returnsLoginEndpoints() throws Exception {
        String[] excludedPaths = new TokenAuthenticator().excludedPaths();

        Assert.assertEquals(excludedPaths, PasswordLogin.EXCLUDED_PATHS);
    }

    private void initTokenCipher() throws Exception {
        CryptoImpl crypto = new CryptoImpl();
        PasswordLogin.tokenCipher = crypto.getCipher(crypto.generateKey()); // 随机生成合法 Base64 密钥
    }
}
