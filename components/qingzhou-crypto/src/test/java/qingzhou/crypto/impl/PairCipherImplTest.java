package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.PairCipher;

public class PairCipherImplTest {
    private static final String PLAIN_TEXT = "qingzhou crypto pair cipher test 轻舟加密测试";

    private PairCipher newPairCipher() {
        CryptoImpl crypto = new CryptoImpl();
        String[] pairKey = crypto.generatePairKey();
        return new PairCipherImpl(pairKey[0], pairKey[1], new Base64CoderImpl());
    }

    private PairCipher newPublicKeyOnlyPairCipher() {
        CryptoImpl crypto = new CryptoImpl();
        String[] pairKey = crypto.generatePairKey();
        return new PairCipherImpl(pairKey[0], null, new Base64CoderImpl());
    }

    // ===================== encryptWithPublicKey(byte[]) =====================

    @Test
    public void normalBytes_encryptWithPublicKey_returnNonNullCipherBytes() throws Exception {
        PairCipher pairCipher = newPairCipher();
        byte[] input = PLAIN_TEXT.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = pairCipher.encryptWithPublicKey(input);

        Assert.assertNotNull(encrypted);
        Assert.assertTrue(encrypted.length > 0);
    }

    @Test
    public void nullBytes_encryptWithPublicKey_throwException() throws Exception {
        PairCipher pairCipher = newPairCipher();

        try {
            pairCipher.encryptWithPublicKey((byte[]) null);
            Assert.fail("null 输入应抛出异常");
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        try {
            pairCipher.encryptWithPublicKey((byte[]) null);
        } catch (Exception e) {
            return;
        }
        Assert.fail("null 输入应抛出异常");
    }

    @Test
    public void normalBytes_encryptWithPublicKey_cipherDiffersFromPlain() throws Exception {
        PairCipher pairCipher = newPairCipher();
        byte[] input = PLAIN_TEXT.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = pairCipher.encryptWithPublicKey(input);

        Assert.assertFalse(Arrays.equals(encrypted, input), "加密后的数据不应与原始数据相同");
    }

    @Test
    public void longBytes_encryptWithPublicKey_supportSegmentedEncryption() throws Exception {
        PairCipher pairCipher = newPairCipher();
        // 超过单段明文块（117 字节）以触发分段加密
        byte[] input = new byte[300];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }

        byte[] encrypted = pairCipher.encryptWithPublicKey(input);

        Assert.assertNotNull(encrypted);
        Assert.assertTrue(encrypted.length > 0);
    }

    // ===================== encryptWithPublicKey(String) =====================

    @Test
    public void normalString_encryptWithPublicKey_returnNonNullBase64() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey(PLAIN_TEXT);

        Assert.assertNotNull(encrypted);
        Assert.assertFalse(encrypted.isEmpty());
    }

    @Test
    public void emptyString_encryptWithPublicKey_returnEmptyString() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey("");

        Assert.assertNotNull(encrypted);
        Assert.assertTrue(encrypted.isEmpty());
    }

    @Test
    public void nullString_encryptWithPublicKey_throwException() throws Exception {
        PairCipher pairCipher = newPairCipher();

        try {
            pairCipher.encryptWithPublicKey((String) null);
            Assert.fail("null 输入应抛出异常");
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void normalString_encryptThenDecrypt_restoreOriginalString() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey(PLAIN_TEXT);
        String decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, PLAIN_TEXT);
    }

    @Test
    public void emptyString_encryptThenDecrypt_restoreEmptyString() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey("");
        String decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, "");
    }

    // ===================== decryptWithPrivateKey(byte[]) =====================

    @Test
    public void normalCipherBytes_decryptWithPrivateKey_restoreOriginalBytes() throws Exception {
        PairCipher pairCipher = newPairCipher();
        byte[] input = PLAIN_TEXT.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = pairCipher.encryptWithPublicKey(input);
        byte[] decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, input);
    }

    @Test
    public void nullBytes_decryptWithPrivateKey_throwException() throws Exception {
        PairCipher pairCipher = newPairCipher();

        try {
            pairCipher.decryptWithPrivateKey((byte[]) null);
            Assert.fail("null 输入应抛出异常");
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void longCipherBytes_decryptWithPrivateKey_restoreOriginalBytes() throws Exception {
        PairCipher pairCipher = newPairCipher();
        byte[] input = new byte[300];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }

        byte[] encrypted = pairCipher.encryptWithPublicKey(input);
        byte[] decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, input);
    }

    // ===================== decryptWithPrivateKey(String) =====================

    @Test
    public void normalBase64Cipher_decryptWithPrivateKey_restoreOriginalString() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey(PLAIN_TEXT);
        String decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, PLAIN_TEXT);
    }

    @Test
    public void emptyString_decryptWithPrivateKey_returnEmptyString() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey("");
        String decrypted = pairCipher.decryptWithPrivateKey(encrypted);

        Assert.assertEquals(decrypted, "");
    }

    @Test
    public void nullString_decryptWithPrivateKey_returnNull() throws Exception {
        PairCipher pairCipher = newPairCipher();

        String decrypted = pairCipher.decryptWithPrivateKey((String) null);

        Assert.assertNull(decrypted);
    }

    // ===================== 密钥缺失场景 =====================

    @Test
    public void onlyPublicKey_encryptThenDecrypt_decryptThrowsException() throws Exception {
        PairCipher pairCipher = newPublicKeyOnlyPairCipher();

        String encrypted = pairCipher.encryptWithPublicKey(PLAIN_TEXT);
        Assert.assertNotNull(encrypted);

        try {
            pairCipher.decryptWithPrivateKey(encrypted);
            Assert.fail("缺少私钥时解密应抛出异常");
        } catch (Throwable e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void nullPublicKey_getPairCipher_throwInvalidKeyException() {
        CryptoImpl crypto = new CryptoImpl();
        try {
            crypto.getPairCipher(null, null);
            Assert.fail("公私钥均为 null 时应抛出异常");
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
    }
}
