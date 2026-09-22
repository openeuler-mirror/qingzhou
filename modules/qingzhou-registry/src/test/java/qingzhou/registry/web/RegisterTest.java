package qingzhou.registry.web;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RegisterTest {
    private static final byte[] REGISTER_TOKEN = "token-1234567890".getBytes(StandardCharsets.UTF_8);

    @Test
    public void matchingToken_register_authorized() {
        Assert.assertTrue(isAuthorized(REGISTER_TOKEN, "token-1234567890"));
    }

    @Test
    public void wrongToken_register_rejected() {
        Assert.assertFalse(isAuthorized(REGISTER_TOKEN, "wrong-token"));
    }

    @Test
    public void missingTokenConfig_register_rejected() {
        Assert.assertFalse(isAuthorized(null, "token-1234567890"));
    }

    private boolean isAuthorized(byte[] registerToken, String presentedToken) {
        try {
            Register register = new Register();
            Method method = Register.class.getDeclaredMethod("isAuthorized", byte[].class, String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(register, registerToken, presentedToken);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
