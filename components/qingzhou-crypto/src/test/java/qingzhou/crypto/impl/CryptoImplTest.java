package qingzhou.crypto.impl;

import java.security.InvalidKeyException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;

public class CryptoImplTest {
    @Test
    public void anyTime_generateKey_mustBe24Chars() {
        for (int i = 0; i < 10000; i++) {
            String key = new CryptoImpl().generateKey();
            Assert.assertNotNull(key);
            Assert.assertFalse(key.matches(".*\\s.*"));
            Assert.assertSame(key.length(), 24);
        }
    }

    @Test
    public void multipleCalls_generateKey_returnRandom() {
        Set<String> randomKeys = new HashSet<>();
        CryptoImpl crypto = new CryptoImpl();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            String randomKey = crypto.generateKey();
            boolean added = randomKeys.add(randomKey);
            Assert.assertTrue(added);
        }
        Assert.assertEquals(randomKeys.size(), count);
    }

    @Test
    public void not24CharsKey_getCipher_throwInvalidKeyException() {
        CryptoImpl crypto = new CryptoImpl();
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
        CryptoImpl crypto = new CryptoImpl();
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
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
}
