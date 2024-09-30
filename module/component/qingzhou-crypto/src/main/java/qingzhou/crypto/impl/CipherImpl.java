package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Cipher;

class CipherImpl implements Cipher {
    private final Base64Coder base64Coder;
    private final String transformation = "DESede"; // Triple-DES encryption algorithm

    static byte[] build3desData(byte[] keySeed) {
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

    CipherImpl(String key, Base64Coder base64Coder) {
        this(key.getBytes(StandardCharsets.UTF_8), base64Coder);
    }

    CipherImpl(byte[] realKeyBytes, Base64Coder base64Coder) {
        this.base64Coder = base64Coder;
        byte[] keySeed = build3desData(realKeyBytes);
        K = new SecretKeySpec(keySeed, transformation);
    }

    @Override
    public String encrypt(String s) throws Exception {
        if (s == null) return null;
        s = s.trim();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] encrypt = encrypt(bytes);
        return base64Coder.encode(encrypt);// 不要用base64，不利于http传输，转义会错误
    }

    @Override
    public String decrypt(String s) throws Exception {
        if (s == null) return null;

        byte[] bytes = base64Coder.decode(s);
        byte[] decrypt = decrypt(bytes);
        return new String(decrypt, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] s) throws Exception {
        if (s == null) return null;

        javax.crypto.Cipher cipher = getCipher();
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, K);
        return cipher.doFinal(s);
    }

    @Override
    public byte[] decrypt(byte[] s) throws Exception {
        if (s == null) {
            return null;
        }

        javax.crypto.Cipher cipher = getCipher();
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, K);
        return cipher.doFinal(s);
    }

    private javax.crypto.Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        try {
            return javax.crypto.Cipher.getInstance(transformation, "SunJCE");
        } catch (Exception e) {
            return javax.crypto.Cipher.getInstance(transformation);
        }
    }
}
