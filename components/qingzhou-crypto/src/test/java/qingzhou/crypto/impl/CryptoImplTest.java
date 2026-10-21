package qingzhou.crypto.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;
import qingzhou.logger.impl.LoggerImpl;

public class CryptoImplTest {
    @Test
    public void anyTime_generateKey_mustBe24Chars() {

        for (int i = 0; i < 10000; i++) {
            String key = crypto.generateCipherKey();
            Assert.assertNotNull(key);
            Assert.assertFalse(key.matches(".*\\s.*"));
            Assert.assertEquals(key.length(), 24);
        }
    }

    private static CryptoImpl crypto;

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    static CryptoImpl getCryptoImpl() {
        if (crypto == null) {
            crypto = new CryptoImpl();
            crypto.init(new HashMap<String, String>() {{
                put("qingzhou.instance", new File("/tmp").getAbsolutePath());
                put("qingzhou.version", "1.0");
            }});

            try {
                LoggerImpl logger = new LoggerImpl();
                setField(crypto, "logger", logger);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return crypto;
    }

    @Test
    public void multipleCalls_generateKey_returnRandom() {
        Set<String> randomKeys = new HashSet<>();
        CryptoImpl crypto = getCryptoImpl();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            String randomKey = crypto.generateCipherKey();
            boolean added = randomKeys.add(randomKey);
            Assert.assertTrue(added);
        }
        Assert.assertEquals(randomKeys.size(), count);
    }

    @Test
    public void not24CharsKey_getCipher_throwInvalidKeyException() {
        CryptoImpl crypto = getCryptoImpl();
        try {
            crypto.getCipher(null);
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
        try {
            crypto.getCipher("");
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
        try {
            crypto.getCipher(" ");
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
        try {
            crypto.getCipher("not be 24 chars");
            Assert.fail();
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
    }

    @Test
    public void use24CharsKey_getCipher_success() {
        CryptoImpl crypto = getCryptoImpl();
        String key = crypto.generateCipherKey();
        try {
            Cipher cipher = crypto.getCipher(key);
            Cipher cipher2 = crypto.getCipher(key);
            Assert.assertNotNull(cipher);
            Assert.assertNotNull(cipher2);
            Assert.assertNotSame(cipher, cipher2);
        } catch (Throwable e) {
            Assert.fail();
        }
    }

    @Test
    public void notAesKeyLength_getCipher_throwInvalidKeyException() {
        CryptoImpl crypto = getCryptoImpl();
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 24); // 24 字符 Base64 可解出 18 字节
        try {
            crypto.getCipher(key);
            Assert.fail("非 AES 密钥长度应在构造时即被拒绝");
        } catch (Throwable e) {
            Assert.assertTrue(e instanceof InvalidKeyException);
        }
    }
}
