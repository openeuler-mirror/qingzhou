package qingzhou.registry.web;

import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RegisterTest {
    private static final byte[] REGISTER_TOKEN = "token-1234567890".getBytes(StandardCharsets.UTF_8);

    @Test
    public void matchingToken_register_authorized() {
        Assert.assertTrue(Register.isAuthorized(REGISTER_TOKEN, "token-1234567890"));
    }

    @Test
    public void wrongToken_register_rejected() {
        Assert.assertFalse(Register.isAuthorized(REGISTER_TOKEN, "wrong-token"));
    }

    @Test
    public void missingTokenConfig_register_rejected() {
        Assert.assertFalse(Register.isAuthorized(null, "token-1234567890"));
    }
}
