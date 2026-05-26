package qingzhou.crypto.impl;

import java.security.InvalidKeyException;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

import qingzhou.crypto.Cipher;

public class CipherImpl implements Cipher {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private final SecretKeySpec keySpec;

    CipherImpl(String key) throws InvalidKeyException {
        if (key.length() != 24) {
            throw new InvalidKeyException("Key must be 24 characters");
        }
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String s) throws Exception {
        byte[] encrypted = encrypt(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
    public String decrypt(String s) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(s);
        return new String(decrypt(decoded), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] data) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    @Override
    public byte[] decrypt(byte[] data) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }
}