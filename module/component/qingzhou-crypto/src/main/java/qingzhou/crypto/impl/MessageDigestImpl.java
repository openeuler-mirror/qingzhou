package qingzhou.crypto.impl;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.MessageDigest;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class MessageDigestImpl implements MessageDigest {
    private final Base64Coder base64Coder;
    private final Random random = ThreadLocalRandom.current();

    private final String SP = "$";

    MessageDigestImpl(Base64Coder base64Coder) {
        this.base64Coder = base64Coder;
    }

    @Override
    public String digest(String text, String algorithm, int saltLength, int iterations) {
        byte[] salt = new byte[0];
        if (saltLength > 0) {
            salt = new byte[saltLength];
            random.nextBytes(salt);
        }
        return digest(text, algorithm, salt, iterations);
    }

    @Override
    public boolean matches(String text, String msgDigest) {
        if (text == null || msgDigest == null) {
            return false;
        }

        String[] splitPwd = msgDigest.split("\\" + SP);

        String algorithm = splitPwd[0];
        byte[] salt = decode(splitPwd[1]);
        int iterations = Integer.parseInt(splitPwd[2]);
        String digest = digest(text, algorithm, salt, iterations);
        return Objects.equals(digest, msgDigest);
    }

    @Override
    public String fingerprint(String data) {
        return digest(data, "MD5", 0, 1);
    }

    private String digest(String inputCredentials, String algorithm, byte[] salt, int iterations) {
        byte[] digest = digest(algorithm, iterations, salt, inputCredentials.getBytes(StandardCharsets.UTF_8));
        String pwd = encode(digest);
        return algorithm + SP + encode(salt) + SP + iterations + SP + pwd;
    }

    private byte[] decode(String encode) {
        return base64Coder.decode(encode);
    }

    private String encode(byte[] bytes) {
        return base64Coder.encode(bytes);
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
        if (iterations > 1) {
            for (int i = 1; i < iterations; i++) {
                md.update(result);
                result = md.digest();
            }
        }

        return result;
    }
}
