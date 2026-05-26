package qingzhou.crypto.impl;

import java.security.SecureRandom;
import java.util.Base64;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Crypto;

public class CryptoImpl implements Crypto {
    private final SecureRandom random = new SecureRandom();
    private final Base64Coder base64Coder = new Base64CoderImpl();
    private final Base32Coder base32Coder = new Base32CoderImpl();
    private final Base16Coder base16Coder = new Base16CoderImpl();
    private final MessageDigestImpl messageDigest = new MessageDigestImpl();
    private final TotpCipherImpl totpCipher = new TotpCipherImpl();

    @Override
    public String generateKey() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        if (base64.length() >= 24) return base64.substring(0, 24);
        StringBuilder sb = new StringBuilder(base64);
        while (sb.length() < 24) sb.append('0');
        return sb.substring(0, 24);
    }

    @Override
    public qingzhou.crypto.Cipher getCipher(String key) throws Exception {
        return new CipherImpl(key);
    }

    @Override
    public String[] generatePairKey() {
        try {
            java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            java.security.KeyPair kp = kpg.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
            return new String[]{publicKey, privateKey};
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    @Override
    public qingzhou.crypto.PairCipher getPairCipher(String publicKey, String privateKey) throws Exception {
        return new PairCipherImpl(publicKey, privateKey);
    }

    @Override
    public qingzhou.crypto.TotpCipher getTotpCipher() {
        return totpCipher;
    }

    @Override
    public qingzhou.crypto.MessageDigest getMessageDigest() {
        return messageDigest;
    }

    @Override
    public Base64Coder getBase64Coder() {
        return base64Coder;
    }

    @Override
    public Base32Coder getBase32Coder() {
        return base32Coder;
    }

    @Override
    public Base16Coder getBase16Coder() {
        return base16Coder;
    }
}