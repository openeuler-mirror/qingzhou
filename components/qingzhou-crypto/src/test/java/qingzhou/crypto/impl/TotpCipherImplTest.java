package qingzhou.crypto.impl;

import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.TotpCipher;

public class TotpCipherImplTest {

    // Base32 合法字符：大写 A-Z、数字 2-7、填充符 =
    private static final Pattern BASE32_VALID_REGEX = Pattern.compile("^[A-Z2-7=]+$");
    // 8 字节密钥 -> Base32 固定 16 位（13 个有效字符 + 3 个填充符 =）
    private static final int KEY_LENGTH = 16;

    // 每个用例独立创建实例，避免共享状态
    private TotpCipher newTotp() {
        return new TotpCipherImpl(new Base16CoderImpl(), new Base32CoderImpl());
    }

    // ===================== generateKey() =====================
    @Test
    public void multipleCalls_generateKey_returnDifferentKeys() {
        TotpCipher totp = newTotp();
        Assert.assertNotEquals(totp.generateKey(), totp.generateKey());
    }

    @Test
    public void generatedKey_generateKey_matchBase32Spec() {
        String key = newTotp().generateKey();
        Assert.assertTrue(BASE32_VALID_REGEX.matcher(key).matches(), "密钥包含非法 Base32 字符: " + key);
    }

    @Test
    public void generatedKey_generateKey_returnCorrectLength() {
        String key = newTotp().generateKey();
        Assert.assertEquals(key.length(), KEY_LENGTH);
    }

    // ===================== getCode(String key) =====================
    @Test
    public void validKey_getCode_returnSixDigitCode() throws Exception {
        TotpCipher totp = newTotp();
        String code = totp.getCode(totp.generateKey());
        Assert.assertNotNull(code);
        Assert.assertEquals(code.length(), 6);
    }

    @Test
    public void sameKeyAndWindow_getCode_returnSameCode() throws Exception {
        TotpCipher totp = newTotp();
        String key = totp.generateKey();
        Assert.assertEquals(totp.getCode(key), totp.getCode(key));
    }

    @Test
    public void generatedCode_getCode_matchSixDigitFormat() throws Exception {
        TotpCipher totp = newTotp();
        String code = totp.getCode(totp.generateKey());
        Assert.assertTrue(code.matches("\\d{6}"), "验证码不是 6 位数字: " + code);
    }

    // ===================== verifyCode(String key, String code) =====================
    @Test
    public void correctCode_verifyCode_returnTrue() throws Exception {
        TotpCipher totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        Assert.assertTrue(totp.verifyCode(key, code));
    }

    @Test
    public void wrongCode_verifyCode_returnFalse() throws Exception {
        TotpCipher totp = newTotp();
        String key = totp.generateKey();
        String code = totp.getCode(key);
        // 翻转最后一位，保证与正确验证码不同
        char last = code.charAt(5);
        String wrong = code.substring(0, 5) + (last == '0' ? '1' : '0');
        Assert.assertFalse(totp.verifyCode(key, wrong));
    }

    @Test
    public void nullKey_verifyCode_returnFalse() throws Exception {
        Assert.assertFalse(newTotp().verifyCode(null, "123456"));
    }

    @Test
    public void nullCode_verifyCode_returnFalse() throws Exception {
        TotpCipher totp = newTotp();
        Assert.assertFalse(totp.verifyCode(totp.generateKey(), null));
    }

    @Test
    public void emptyKeyOrCode_verifyCode_returnFalse() throws Exception {
        TotpCipher totp = newTotp();
        String key = totp.generateKey();
        Assert.assertFalse(totp.verifyCode("", ""));
        Assert.assertFalse(totp.verifyCode(key, ""));
    }
}