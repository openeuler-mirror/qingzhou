package qingzhou.crypto.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.PairCipher;

class PairCipherImpl implements PairCipher {
    static final String ALG = "RSA";
    private static final int ENCRYPT_BLOCK = 117;
    private static final int DECRYPT_BLOCK = 128;
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
    public String encryptWithPublicKey(String input) {
        try {
            return encryptWithKey(publicKey, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decryptWithPrivateKey(String input) {
        try {
            return decryptWithKey(privateKey, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String encryptWithPrivateKey(String input) {
        try {
            return encryptWithKey(privateKey, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decryptWithPublicKey(String input) {
        try {
            return decryptWithKey(publicKey, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        byte[] bytesContent = input.getBytes(StandardCharsets.UTF_8);
        byte[] enContent = encryptWithKey(key, bytesContent);
        return base64Coder.encode(enContent);
    }

    private byte[] encryptWithKey(Key key, byte[] encryptBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        //分段加密
        return cipherBytes(encryptBytes, cipher, ENCRYPT_BLOCK);
    }

    private String decryptWithKey(Key key, String input) throws Exception {
        if (input == null) return null;

        byte[] bytesContent = base64Coder.decode(input);
        byte[] decryptedWithKey = decryptWithKey(key, bytesContent);
        return new String(decryptedWithKey, StandardCharsets.UTF_8);
    }

    private byte[] decryptWithKey(Key key, byte[] decryptBytes) throws Exception {
        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.DECRYPT_MODE, key);

        return cipherBytes(decryptBytes, cipher, DECRYPT_BLOCK);
    }

    private byte[] cipherBytes(byte[] decryptBytes, Cipher cipher, int decryptBlock) throws IllegalBlockSizeException, BadPaddingException, IOException {
        int decryptLen = decryptBytes.length;
        int offLen = 0;
        int i = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (offLen < decryptLen) {
            byte[] cache;
            if (decryptLen - offLen > decryptBlock) {
                cache = cipher.doFinal(decryptBytes, offLen, decryptBlock);
            } else {
                cache = cipher.doFinal(decryptBytes, offLen, decryptLen - offLen);
            }
            bos.write(cache);
            i++;
            offLen = decryptBlock * i;
        }

        return bos.toByteArray();
    }
}
