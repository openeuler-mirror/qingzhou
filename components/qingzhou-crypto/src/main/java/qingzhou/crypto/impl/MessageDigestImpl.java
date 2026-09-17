package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.MessageDigest;

class MessageDigestImpl implements MessageDigest {
    private static final SecureRandom random = new SecureRandom();
    private static final String SALT_SEPARATOR = "$";

    private final Base16Coder base16Coder;

    MessageDigestImpl(Base16Coder base16Coder) {
        this.base16Coder = base16Coder;
    }

    @Override
    public String digest(String text, String algorithm, int saltLength, int iterations) {
        byte[] salt = null;
        if (saltLength > 0) {
            salt = new byte[saltLength];
            random.nextBytes(salt);
        }
        return mutate(text, algorithm, salt, iterations);
    }

    @Override
    public boolean matches(String text, String msgDigest) {
        if (text == null || msgDigest == null) {
            return false;
        }

        String[] splitPwd = msgDigest.split("\\" + SALT_SEPARATOR);

        String algorithm = splitPwd[0];
        byte[] salt = decode(splitPwd[1]);
        int iterations = Integer.parseInt(splitPwd[2]);
        String digest = mutate(text, algorithm, salt, iterations);
        // 常量时间比较，防止时序侧信道推算摘要
        return java.security.MessageDigest.isEqual(digest.getBytes(StandardCharsets.UTF_8),
                msgDigest.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String md5(String data) {
        byte[] digest = md5(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
        return encode(digest);
    }

    @Override
    public byte[] md5(byte[] data) {
        return digest("MD5", 1, data);
    }

    @Override
    public String sha256(String data) {
        byte[] digest = sha256(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
        return encode(digest);
    }

    @Override
    public byte[] sha256(byte[] data) {
        return digest("SHA-256", 1, data);
    }

    private String mutate(String data, String algorithm, byte[] salt, int iterations) {
        if (salt == null) {
            salt = new byte[0];
        }
        byte[] digest = digest(algorithm, iterations, salt,
                data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
        String pwd = encode(digest);
        return algorithm + SALT_SEPARATOR + encode(salt) + SALT_SEPARATOR + iterations + SALT_SEPARATOR + pwd;
    }

    private byte[] decode(String encode) {
        return base16Coder.decode(encode);
    }

    private String encode(byte[] bytes) {
        return base16Coder.encode(bytes);
    }

    private byte[] digest(String algorithm, int iterations, byte[]... input) {
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        // Round 1
        for (byte[] bytes : input) {
            if (bytes != null && bytes.length > 0) {
                md.update(bytes);
            }
        }
        byte[] result = md.digest();

        // Subsequent rounds
        for (int i = 0; i < iterations - 1; i++) {
            md.update(result);
            result = md.digest();
        }

        return result;
    }
}
