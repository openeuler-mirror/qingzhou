package qingzhou.crypto.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.crypto.Cipher;

public class CipherImplTest {
    @Test
    public void nullStringParams_encrypt_returnNull() {
        doTest(cipher -> {
            String encrypted = cipher.encrypt((String) null);
            Assert.assertNull(encrypted);
        });
    }


    @Test
    public void emptyStringParams_encrypt_success() {
        doTest(cipher -> {
            String encrypted = cipher.encrypt((String) "");
            Assert.assertNotNull(encrypted);
            Assert.assertFalse(encrypted.trim().isEmpty());
            String decrypt = cipher.decrypt(encrypted);
            Assert.assertEquals(decrypt, "");
        });
    }


    @Test
    public void normalStringParams_encrypt_success() {
        doTest(cipher -> {
            String testString = "my-password";
            String encrypted = cipher.encrypt(testString);
            Assert.assertNotNull(encrypted);
            Assert.assertFalse(encrypted.trim().isEmpty());
            String decrypt = cipher.decrypt(encrypted);
            Assert.assertEquals(decrypt, testString);
        });
    }

    void doTest(TestLogicNormal testLogicNormal) {
        CryptoImpl crypto = new CryptoImpl();
        String key = crypto.generateKey();
        try {
            Cipher cipher = crypto.getCipher(key);
            testLogicNormal.run(cipher);
        } catch (Throwable e) {
            Assert.fail();
        }
    }

    interface TestLogicNormal {
        void run(Cipher cipher) throws Throwable;
    }
}
