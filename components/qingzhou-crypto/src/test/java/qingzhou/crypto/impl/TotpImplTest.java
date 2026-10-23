package qingzhou.crypto.impl;

import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Totp;

public class TotpImplTest {

    // Base32 合法字符：大写 A-Z、数字 2-7、填充符 =
    private static final Pattern BASE32_VALID_REGEX = Pattern.compile("^[A-Z2-7=]+$");

    // 每个用例独立创建实例，避免共享状态
    private TotpImpl newTotp() {
        return new TotpImpl(new Base16CoderImpl(), new Base32CoderImpl());
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

    // ===================== 内置窗口容忍：返回命中的时间步 =====================
    @Test
    public void previousWindowCode_matchWindow_returnPreviousStep() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long step = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(totp.matchWindow(key, totp.getCode(key, step - 1)) == step - 1); // 窗口末尾读到口令、跨窗后才提交
    }

    @Test
    public void nextWindowCode_matchWindow_returnNextStep() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long step = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(totp.matchWindow(key, totp.getCode(key, step + 1)) == step + 1); // 覆盖服务端时钟偏慢
    }

    @Test
    public void outOfRangeCode_matchWindow_returnMinusOne() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long step = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(totp.matchWindow(key, totp.getCode(key, step - 2)) == -1);
        Assert.assertTrue(totp.matchWindow(key, totp.getCode(key, step + 2)) == -1);
    }

    // ===================== verifyCode(String key, String code) =====================
    @Test
    public void validCode_verifyCode_returnTrue() throws Exception {
        Totp totp = newTotp();
        String key = totp.generateKey();
        Assert.assertTrue(totp.verifyCode(key, totp.getCode(key)));
    }

    @Test
    public void outOfRangeCode_verifyCode_returnFalse() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        long step = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertFalse(totp.verifyCode(key, totp.getCode(key, step - 2)));
    }

    // ===================== verifyCodeWithReplayGuard(String key, String code) =====================
    @Test
    public void sameCode_verifyCodeWithReplayGuard_onlyAcceptedOnce() throws Exception {
        Totp totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key, code));
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, code)); // 同窗口重放
        Assert.assertFalse(totp.verifyCodeWithReplayGuard(key, totp.getCode(key))); // 生成新码但仍是同一窗口
    }

    @Test
    public void differentKeys_verifyCodeWithReplayGuard_stateIndependent() throws Exception {
        Totp totp = newTotp();
        String key1 = totp.generateKey();
        String key2 = totp.generateKey();
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key1, totp.getCode(key1)));
        Assert.assertTrue(totp.verifyCodeWithReplayGuard(key2, totp.getCode(key2)));
    }

    // ===================== matchWindow(String key, String code) =====================
    @Test
    public void correctCode_matchWindow_returnCurrentStep() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        long step = totp.matchWindow(key, code);
        // 校验期间可能跨窗口，命中当前或相邻窗口都算正确
        long now = System.currentTimeMillis() / TotpImpl.STEP_MILLIS;
        Assert.assertTrue(step >= now - 1 && step <= now + 1, "命中时间步异常: " + step);
    }

    @Test
    public void wrongCode_matchWindow_returnMinusOne() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        // 翻转最后一位，保证与正确验证码不同
        char last = code.charAt(5);
        String wrong = code.substring(0, 5) + (last == '0' ? '1' : '0');
        Assert.assertTrue(totp.matchWindow(key, wrong) == -1);
    }

    @Test
    public void nullKey_matchWindow_returnMinusOne() throws Exception {
        Assert.assertTrue(newTotp().matchWindow(null, "123456") == -1);
    }

    @Test
    public void nullCode_matchWindow_returnMinusOne() throws Exception {
        TotpImpl totp = newTotp();
        Assert.assertTrue(totp.matchWindow(totp.generateKey(), null) == -1);
    }

    @Test
    public void emptyKeyOrCode_matchWindow_returnMinusOne() throws Exception {
        TotpImpl totp = newTotp();
        String key = totp.generateKey();
        Assert.assertTrue(totp.matchWindow("", "") == -1);
        Assert.assertTrue(totp.matchWindow(key, "") == -1);
    }
}
