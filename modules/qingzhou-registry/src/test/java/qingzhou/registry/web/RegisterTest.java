package qingzhou.registry.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import qingzhou.crypto.Crypto;
import qingzhou.crypto.impl.CryptoImpl;
import qingzhou.http.server.HttpResponse;
import qingzhou.registry.impl.Register;

public class RegisterTest {
    private static final byte[] REGISTER_TOKEN = "token-1234567890".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    // 记录型响应桩：断言服务端对非法注册的响应内容与状态码
    private static class RecordingResponse implements HttpResponse {
        final List<String> sent = new ArrayList<>();
        Integer status;

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
            sent.add(bodyAsUtf8);
            return this;
        }

        @Override
        public HttpResponse send(byte[] body) {
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public void redirect(String url) {
        }
    }

    private static String[] validParts(String token, String nonce) {
        return new String[]{token, String.valueOf(System.currentTimeMillis()), nonce, "proof", "{}"};
    }

    private static boolean invokeIsValid(Register register, String[] parts, HttpResponse response) throws Exception {
        Method method = Register.class.getDeclaredMethod("isValid", String[].class, HttpResponse.class);
        method.setAccessible(true);
        return (boolean) method.invoke(register, parts, response);
    }

    private static Register newRegister(byte[] token) throws Exception {
        Register register = new Register();
        Field tokenField = Register.class.getDeclaredField("registerToken");
        tokenField.setAccessible(true);
        tokenField.set(register, token);
        Field cryptoField = Register.class.getDeclaredField("crypto");
        cryptoField.setAccessible(true);
        cryptoField.set(register, (Crypto) new CryptoImpl());
        return register;
    }

    @Test
    public void validRegistration_passesAllGates() throws Exception {
        Register register = newRegister(REGISTER_TOKEN);
        RecordingResponse response = new RecordingResponse();

        Assert.assertTrue(invokeIsValid(register, validParts("token-1234567890", UUID.randomUUID().toString()), response));
        Assert.assertTrue(response.sent.isEmpty());
    }

    @Test
    public void wrongToken_register_rejected() throws Exception {
        Register register = newRegister(REGISTER_TOKEN);
        RecordingResponse response = new RecordingResponse();

        Assert.assertFalse(invokeIsValid(register, validParts("wrong-token", UUID.randomUUID().toString()), response));
        Assert.assertEquals(response.sent.get(0), "register_token error");
    }

    @Test
    public void missingTokenConfig_register_rejected() throws Exception {
        Register register = newRegister(null);
        RecordingResponse response = new RecordingResponse();

        Assert.assertFalse(invokeIsValid(register, validParts("token-1234567890", UUID.randomUUID().toString()), response));
        Assert.assertEquals(response.sent.get(0), "register_token error");
    }

    @Test
    public void malformedParts_register_rejected() throws Exception {
        Register register = newRegister(REGISTER_TOKEN);
        RecordingResponse response = new RecordingResponse();

        Assert.assertFalse(invokeIsValid(register, new String[]{"token-1234567890"}, response));
        Assert.assertEquals(response.sent.get(0), "data format error");
    }

    @Test
    public void staleTimestamp_register_rejected() throws Exception {
        Register register = newRegister(REGISTER_TOKEN);
        RecordingResponse response = new RecordingResponse();

        String[] parts = validParts("token-1234567890", UUID.randomUUID().toString());
        parts[1] = String.valueOf(System.currentTimeMillis() - 10 * 60 * 1000); // 超出时间窗

        Assert.assertFalse(invokeIsValid(register, parts, response));
        Assert.assertEquals(response.status, Integer.valueOf(400));
    }

    @Test
    public void replayedNonce_register_rejected() throws Exception {
        Register register = newRegister(REGISTER_TOKEN);
        RecordingResponse response = new RecordingResponse();
        String nonce = UUID.randomUUID().toString();

        Assert.assertTrue(invokeIsValid(register, validParts("token-1234567890", nonce), response));
        Assert.assertFalse(invokeIsValid(register, validParts("token-1234567890", nonce), response));
        Assert.assertEquals(response.status, Integer.valueOf(409));
    }
}
