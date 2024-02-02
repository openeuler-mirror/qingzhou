package qingzhou.crypto.impl;

import qingzhou.crypto.PublicKeyCipher;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class PublicKeyCipherImpl implements PublicKeyCipher {
    static final String ALG = "RSA";
    private static final int ENCRYPT_BLOCK = 117;
    private static final int DECRYPT_BLOCK = 128;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public PublicKeyCipherImpl(String pubKeyAsBase64, String priKeyAsBase64) {
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
        byte[] pubBytes = Hex.hexToBytes(keyAsBase64);
        X509EncodedKeySpec encPubKeySpec = new X509EncodedKeySpec(pubBytes);
        return KeyFactory.getInstance(ALG).generatePublic(encPubKeySpec);
    }

    private PrivateKey convertPrivate(String keyAsBase64) throws Exception {
        byte[] privateBytes = Hex.hexToBytes(keyAsBase64);
        PKCS8EncodedKeySpec encPriKeySpec = new PKCS8EncodedKeySpec(privateBytes);
        return KeyFactory.getInstance(ALG).generatePrivate(encPriKeySpec);
    }

    private String encryptWithKey(Key key, String input) throws Exception {
        byte[] bytesContent = input.getBytes(StandardCharsets.UTF_8);
        byte[] enContent = encryptWithKey(key, bytesContent);
        return Hex.bytesToHex(enContent);
    }

    private byte[] encryptWithKey(Key key, byte[] bytesContent) throws Exception {
        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        //分段加密
        int inputLen = bytesContent.length;
        int offLen = 0;//偏移量
        int i = 0;
        ByteArrayOutputStream bops = new ByteArrayOutputStream();
        while (offLen < inputLen) {
            byte[] cache;
            if (inputLen - offLen > ENCRYPT_BLOCK) {
                cache = cipher.doFinal(bytesContent, offLen, ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(bytesContent, offLen, inputLen - offLen);
            }
            bops.write(cache);
            i++;
            offLen = ENCRYPT_BLOCK * i;
        }
        return bops.toByteArray();
    }

    private String decryptWithKey(Key key, String input) throws Exception {
        if (input == null) return null;

        byte[] bytesContent;
        try {
            bytesContent = Hex.hexToBytes(input);// 内部是十六进制编码
        } catch (Exception e) {
            bytesContent = decodeAsBase64(input);// 登录页面是base64编码
        }
        byte[] decryptedWithKey = decryptWithKey(key, bytesContent);
        return new String(decryptedWithKey, StandardCharsets.UTF_8);
    }

    private byte[] decodeAsBase64(String src) {
        return java.util.Base64.getDecoder().decode(src);
    }

    private byte[] decryptWithKey(Key key, byte[] bytesContent) throws Exception {
        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.DECRYPT_MODE, key);

        int inputLen = bytesContent.length;
        int offLen = 0;
        int i = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (offLen < inputLen) {
            byte[] cache;
            if (inputLen - offLen > DECRYPT_BLOCK) {
                cache = cipher.doFinal(bytesContent, offLen, DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(bytesContent, offLen, inputLen - offLen);
            }
            bos.write(cache);
            i++;
            offLen = DECRYPT_BLOCK * i;
        }

        return bos.toByteArray();
    }
}
