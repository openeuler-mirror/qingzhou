package qingzhou.auth.impl;

import java.lang.reflect.Field;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.auth.TokenService;
import qingzhou.http.server.AuthResult;

public class TokenAuthenticatorTest {

    @Test
    public void noAuthorizationHeader_authenticate_returnsMissing() throws Exception {
        PasswordLoginHandlerTest.StubHttpRequest request = new PasswordLoginHandlerTest.StubHttpRequest();
        TokenAuthenticator authenticator = buildAuthenticator(token -> null);

        AuthResult result = authenticator.authenticate(request);

        Assert.assertSame(result.status(), AuthResult.Status.MISSING);
    }

    @Test
    public void nonBearerHeader_authenticate_returnsMissing() throws Exception {
        PasswordLoginHandlerTest.StubHttpRequest request = new PasswordLoginHandlerTest.StubHttpRequest();
        request.header = "Basic abc";
        TokenAuthenticator authenticator = buildAuthenticator(token -> null);

        AuthResult result = authenticator.authenticate(request);

        Assert.assertSame(result.status(), AuthResult.Status.MISSING);
    }

    @Test
    public void validToken_authenticate_returnsPass() throws Exception {
        PasswordLoginHandlerTest.StubHttpRequest request = new PasswordLoginHandlerTest.StubHttpRequest();
        request.header = "Bearer some-token";
        TokenAuthenticator authenticator = buildAuthenticator(token -> "admin");

        AuthResult result = authenticator.authenticate(request);

        Assert.assertSame(result.status(), AuthResult.Status.PASS);
        Assert.assertEquals(result.getPrincipal(), "admin");
    }

    @Test
    public void invalidToken_authenticate_returnsReject() throws Exception {
        PasswordLoginHandlerTest.StubHttpRequest request = new PasswordLoginHandlerTest.StubHttpRequest();
        request.header = "Bearer bad-token";
        TokenAuthenticator authenticator = buildAuthenticator(token -> null);

        AuthResult result = authenticator.authenticate(request);

        Assert.assertSame(result.status(), AuthResult.Status.REJECT);
        Assert.assertEquals(result.getReason(), "invalid token");
    }

    @Test
    public void anyRequest_excludedPaths_returnsLoginEndpoints() throws Exception {
        TokenAuthenticator authenticator = buildAuthenticator(token -> null);

        Assert.assertEquals(authenticator.excludedPaths(), PasswordLoginHandler.EXCLUDED_PATHS);
    }

    private TokenAuthenticator buildAuthenticator(VerifyStub verifyStub) throws Exception {
        TokenAuthenticator authenticator = new TokenAuthenticator();
        Field field = TokenAuthenticator.class.getDeclaredField("tokenService");
        field.setAccessible(true);
        field.set(authenticator, new TokenService() {
            @Override
            public String createToken(String user) {
                return null;
            }

            @Override
            public String verifyToken(String token) {
                return verifyStub.verify(token);
            }
        });
        return authenticator;
    }

    private interface VerifyStub {
        String verify(String token);
    }
}
