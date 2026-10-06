package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Cipher;

class CipherImpl implements Cipher {
    private static final String ALG = "AES";
    private static final String ALG_MODE = ALG + "/GCM/NoPadding";

    private static final int TAG_SIZE = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Base64Coder base64Coder;

    private final SecretKeySpec key;

    CipherImpl(String key, Base64Coder base64Coder) {
        this.key = new SecretKeySpec(base64Coder.decode(key), ALG);
        this.base64Coder = base64Coder;
    }

    @Override
    public String encrypt(String s) throws Exception {
        if (s == null) return null;
        s = s.trim();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] encrypt = encrypt(bytes);
        return base64Coder.encode(encrypt);
    }

    @Override
    public String tryDecrypt(String s, String configName) {
        if (s == null) return null;
        if (s.startsWith(Cipher.PLAIN_PREFIX_MARKER)) {
            return s.substring(Cipher.PLAIN_PREFIX_MARKER.length());
        }

        try {
            return decrypt(s);
        } catch (Exception e) {
            System.err.println("WARNING: >>> ");
            System.err.println("WARNING: >>> failed to decrypt: " + configName + ", use bin/gen-cipher-password.sh to encrypt it");
            System.err.println("WARNING: >>> ");
            return s;
        }
    }

    @Override
    public String decrypt(String s) throws Exception {
        if (s == null) return null;

        byte[] bytes = base64Coder.decode(s);
        byte[] decrypt = decrypt(bytes);
        return new String(decrypt, StandardCharsets.UTF_8);
    }

    /**
     * 加密：字节数组 -> 加密后字节数组(IV+密文+标签)
     */
    @Override
    public byte[] encrypt(byte[] s) throws Exception {
        if (s == null) return null;
        return encrypt(s, 0, s.length);
    }

    @Override
    public byte[] decrypt(byte[] s, byte[] aad) throws Exception {
        if (s == null) return null;

        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(s, 0, iv, 0, IV_SIZE);
        byte[] encrypted = new byte[s.length - IV_SIZE];
        System.arraycopy(s, IV_SIZE, encrypted, 0, encrypted.length);

        javax.crypto.Cipher cipher = getCipher();
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_SIZE * 8, iv));
        if (aad != null) cipher.updateAAD(aad); // AAD 参与认证但不出现在密文中
        return cipher.doFinal(encrypted);
    }

    @Override
    public byte[] encrypt(byte[] s, int off, int len) throws Exception {
        return encrypt(s, off, len, null);
    }

    @Override
    public byte[] encrypt(byte[] s, int off, int len, byte[] aad) throws Exception {
        if (s == null) return null;

        // 生成随机IV
        byte[] iv = new byte[IV_SIZE];
        RANDOM.nextBytes(iv);

        // 执行加密，结果包含：密文+认证标签
        javax.crypto.Cipher cipher = getCipher();
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_SIZE * 8, iv));
        if (aad != null) cipher.updateAAD(aad);
        byte[] ciphertext = cipher.doFinal(s, off, len);

        // 拼接 IV(12) + 密文+标签
        byte[] result = new byte[IV_SIZE + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(ciphertext, 0, result, IV_SIZE, ciphertext.length);
        return result;
    }

    @Override
    public byte[] decrypt(byte[] s) throws Exception {
        if (s == null) {
            return null;
        }

        // 拆分 IV、密文+标签
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(s, 0, iv, 0, IV_SIZE);
        byte[] encrypted = new byte[s.length - IV_SIZE];
        System.arraycopy(s, IV_SIZE, encrypted, 0, encrypted.length);

        // 执行解密+校验，返回原始数据
        javax.crypto.Cipher cipher = getCipher();
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_SIZE * 8, iv));
        return cipher.doFinal(encrypted);
    }

    private javax.crypto.Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return javax.crypto.Cipher.getInstance(ALG_MODE);
    }
}
