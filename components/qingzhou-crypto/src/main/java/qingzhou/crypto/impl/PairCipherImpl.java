package qingzhou.crypto.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.PairCipher;

class PairCipherImpl implements PairCipher {
    static final String ALG = "RSA";
    // 不用默认的 RSA/ECB/PKCS1Padding：PKCS#1 v1.5 在「解密失败可被外部观察」的场景下构成填充预言机
    private static final String TRANSFORM = "RSA/ECB/OAEPPadding";
    private static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    private static final int OAEP_OVERHEAD = 2 * 32 + 2; // OAEP(SHA-256) 固定开销，加密分块须扣除

    private final Base64Coder base64Coder;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    PairCipherImpl(String pubKeyAsBase64, String priKeyAsBase64, Base64Coder base64Coder) {
        this.base64Coder = base64Coder;
        try {
            if (pubKeyAsBase64 != null) {
                publicKey = convertPublic(pubKeyAsBase64);
            }
            if (priKeyAsBase64 != null) {
                privateKey = convertPrivate(priKeyAsBase64);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public byte[] encryptWithPublicKey(byte[] input) throws Exception {
        return encryptWithKey(publicKey, input);
    }

    @Override
    public String encryptWithPublicKey(String input) throws Exception {
        return encryptWithKey(publicKey, input);
    }

    @Override
    public byte[] decryptWithPrivateKey(byte[] input) throws Exception {
        return decryptWithKey(privateKey, input);
    }

    @Override
    public String decryptWithPrivateKey(String input) throws Exception {
        return decryptWithKey(privateKey, input);
    }

    private PublicKey convertPublic(String keyAsBase64) throws Exception {
        if (keyAsBase64 == null) {
            return null;
        }
        byte[] pubBytes = base64Coder.decode(keyAsBase64);
        X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(pubBytes);
        return KeyFactory.getInstance(ALG).generatePublic(encPubKeySpec);
    }

    private PrivateKey convertPrivate(String keyAsBase64) throws Exception {
        byte[] privateBytes = base64Coder.decode(keyAsBase64);
        PKCS8EncodedKeySpec encPriKeySpec = new PKCS8EncodedKeySpec(privateBytes);
        return KeyFactory.getInstance(ALG).generatePrivate(encPriKeySpec);
    }

    private String encryptWithKey(Key key, String input) throws Exception {
        if (input == null) return null;

        byte[] bytesContent = input.getBytes(StandardCharsets.UTF_8);
        byte[] enContent = encryptWithKey(key, bytesContent);
        return base64Coder.encode(enContent);
    }

    private byte[] encryptWithKey(Key key, byte[] input) throws Exception {
        if (input == null) return null;
        return cipherBytes(input, cipher(key, Cipher.ENCRYPT_MODE), modulusBytes(key) - OAEP_OVERHEAD);
    }

    private String decryptWithKey(Key key, String input) throws Exception {
        if (input == null) return null;

        byte[] bytesContent = base64Coder.decode(input);
        byte[] decryptedWithKey = decryptWithKey(key, bytesContent);
        return new String(decryptedWithKey, StandardCharsets.UTF_8);
    }

    private byte[] decryptWithKey(Key key, byte[] input) throws Exception {
        if (input == null) return null;
        return cipherBytes(input, cipher(key, Cipher.DECRYPT_MODE), modulusBytes(key));
    }

    private Cipher cipher(Key key, int mode) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORM);
        cipher.init(mode, key, OAEP_SPEC);
        return cipher;
    }

    // RSA 是非分组密码，Cipher.getBlockSize() 返回 0，故由模长推导分块大小
    private int modulusBytes(Key key) {
        return (((RSAKey) key).getModulus().bitLength() + 7) / 8;
    }

    private byte[] cipherBytes(byte[] input, Cipher cipher, int blockSize) throws IllegalBlockSizeException, BadPaddingException, IOException {
        int inputLen = input.length;
        int offset = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (offset < inputLen) {
            int len = Math.min(blockSize, inputLen - offset);
            bos.write(cipher.doFinal(input, offset, len));
            offset += len;
        }
        return bos.toByteArray();
    }
}
