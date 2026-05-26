package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import qingzhou.crypto.PairCipher;

public class PairCipherImpl implements PairCipher {
    private static final String ALGORITHM = "RSA/ECB/PKCS1Padding";

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    PairCipherImpl(String publicKeyStr, String privateKeyStr) throws Exception {
        if (publicKeyStr != null && !publicKeyStr.isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        } else {
            this.publicKey = null;
        }
        if (privateKeyStr != null && !privateKeyStr.isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
        } else {
            this.privateKey = null;
        }
    }

    @Override
    public byte[] encryptWithPublicKey(byte[] input) throws Exception {
        if (publicKey == null) throw new IllegalStateException("Public key not set");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(input);
    }

    @Override
    public String encryptWithPublicKey(String input) throws Exception {
        byte[] encrypted = encryptWithPublicKey(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
    public byte[] decryptWithPrivateKey(byte[] input) throws Exception {
        if (privateKey == null) throw new IllegalStateException("Private key not set");
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(input);
    }

    @Override
    public String decryptWithPrivateKey(String input) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(input);
        return new String(decryptWithPrivateKey(decoded), StandardCharsets.UTF_8);
    }
}