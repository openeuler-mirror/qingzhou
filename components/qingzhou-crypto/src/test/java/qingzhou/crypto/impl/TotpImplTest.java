package qingzhou.crypto.impl;

import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Totp;

public class TotpImplTest {

    private static final Pattern BASE32_VALID_REGEX = Pattern.compile("^[A-Z2-7=]+$");

    private TotpImpl newTotp() {
        return new TotpImpl(new Base32CoderImpl());
    }

    // ===================== generateKey() =====================
    @Test
    public void multipleCalls_generateKey_returnDifferentKeys() {
        Totp totp = newTotp();
        Assert.assertNotEquals(totp.generateKey(), totp.generateKey());
    }

    @Test
    public void generatedKey_generateKey_matchBase32Spec() {
        String key = newTotp().generateKey();
        Assert.assertTrue(BASE32_VALID_REGEX.matcher(key).matches(), "密钥包含非法 Base32 字符: " + key);
    }

    // ===================== getCode(String key) =====================
    @Test
    public void validKey_getCode_returnSixDigitCode() throws Exception {
        Totp totp = newTotp();
        String code = totp.getCode(totp.generateKey());
        Assert.assertNotNull(code);
        Assert.assertEquals(code.length(), 6);
    }

    @Test
    public void sameKeyAndWindow_getCode_returnSameCode() throws Exception {
        Totp totp = newTotp();
        String key = totp.generateKey();
        Assert.assertEquals(totp.getCode(key), totp.getCode(key));
    }

    @Test
    public void generatedCode_getCode_matchSixDigitFormat() throws Exception {
        Totp totp = newTotp();
        String code = totp.getCode(totp.generateKey());
        Assert.assertTrue(code.matches("\\d{6}"), "验证码不是 6 位数字: " + code);
    }

    // ===================== verifyCodeWithReplayGuard(String key, String code) =====================
    @Test
    public void currentWindowCode_verifyCodeWithReplayGuard_returnTrue() throws Exception {
        Totp totp = newTotp();
        String key = totp.generateKey();
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key, totp.getCode(key)));
    }

    @Test
    public void previousWindowCode_verifyCodeWithReplayGuard_returnTrue() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long now = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key, totp.getCode(key, now - 1))); // 提交延迟会把口令推向前序窗口
    }

    @Test
    public void nextWindowCode_verifyCodeWithReplayGuard_returnTrue() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long now = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key, totp.getCode(key, now + 1))); // 覆盖服务端时钟偏慢
    }

    @Test
    public void outOfRangeCode_verifyCodeWithReplayGuard_returnFalse() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long now = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, totp.getCode(key, now - 100)));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, totp.getCode(key, now + 100)));
    }

    @Test
    public void wrongCode_verifyCodeWithReplayGuard_returnFalse() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        char last = code.charAt(5);
        String wrong = code.substring(0, 5) + (last == '0' ? '1' : '0');
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, wrong));
    }

    @Test
    public void nullOrEmpty_verifyCodeWithReplayGuard_returnFalse() throws Exception {
        Totp totp = newTotp();
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(null, "123456"));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(totp.generateKey(), null));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard("", ""));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(totp.generateKey(), ""));
    }

    @Test
    public void sameWindow_verifyCodeWithReplayGuard_onlyAcceptedOnce() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long window = System.currentTimeMillis() / TotpImpl.STEP_MILLIS; // 固定窗口，避免跨窗导致断言抖动
        String code = totp.getCode(key, window);
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key, code));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, code)); // 同窗口重放
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, totp.getCode(key, window))); // 同窗口再生成仍拒绝
    }

    @Test
    public void differentKeys_verifyCodeWithReplayGuard_stateIndependent() throws Exception {
        Totp totp = newTotp();
        String key1 = totp.generateKey();
        String key2 = totp.generateKey();
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key1, totp.getCode(key1)));
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key2, totp.getCode(key2)));
    }
}
