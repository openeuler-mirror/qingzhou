package qingzhou.config.impl;

import qingzhou.framework.util.HexUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

class CipherInternal {
    private final String transformation = "DESede"; // Triple-DES encryption algorithm

    private byte[] build3desData(byte[] keySeed) {
        // 初始化对称密钥
        byte[] keys = new byte[24];
        if (keySeed.length < keys.length) {
            throw new IllegalArgumentException("The key length is insufficient.");
        }
        System.arraycopy(keySeed, 0, keys, 0, 16);
        System.arraycopy(keySeed, 0, keys, 16, 8);
        return keys;
    }

    private final SecretKeySpec K;

    CipherInternal(byte[] realKeyBytes) {
        byte[] keySeed = build3desData(realKeyBytes);
        K = new SecretKeySpec(keySeed, transformation);
    }

    String encrypt(String s) throws Exception {
        if (s == null) return null;
        s = s.trim();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] encrypt = encrypt(bytes);
        return HexUtil.bytesToHex(encrypt);// 不要用base64，不利于http传输，转义会错误
    }

    String decrypt(String s) throws Exception {
        if (s == null) return null;

        byte[] bytes = HexUtil.hexToBytes(s);
        byte[] decrypt = decrypt(bytes);
        return new String(decrypt, StandardCharsets.UTF_8);
    }

    byte[] encrypt(byte[] s) throws Exception {
        if (s == null) return null;

        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, K);
        return cipher.doFinal(s);
    }

    byte[] decrypt(byte[] s) throws Exception {
        if (s == null) {
            return null;
        }

        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, K);
        return cipher.doFinal(s);
    }

    private Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        try {
            return Cipher.getInstance(transformation, "SunJCE");
        } catch (Exception e) {
            return Cipher.getInstance(transformation);
        }
    }
}
