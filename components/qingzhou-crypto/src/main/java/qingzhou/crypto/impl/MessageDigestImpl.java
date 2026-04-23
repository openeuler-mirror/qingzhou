package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.MessageDigest;

class MessageDigestImpl implements MessageDigest {
    private final Base16Coder base16Coder;
    private final Random random = ThreadLocalRandom.current();

    private final String SP = "$";

    MessageDigestImpl(Base16Coder base16Coder) {
        this.base16Coder = base16Coder;
    }

    @Override
    public String digest(String text, String algorithm, int saltLength, int iterations) {
        byte[] salt = new byte[0];
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

        String[] splitPwd = msgDigest.split("\\" + SP);

        String algorithm = splitPwd[0];
        byte[] salt = decode(splitPwd[1]);
        int iterations = Integer.parseInt(splitPwd[2]);
        String digest = mutate(text, algorithm, salt, iterations);
        return Objects.equals(digest, msgDigest);
    }

    @Override
    public String md5(String data) {
        return digest(data, "MD5");
    }

    @Override
    public String sha256(String data) {
        return digest(data, "SHA-256");
    }

    private String digest(String data, String alg) {
        byte[] digest = digest(alg, 1, data.getBytes(StandardCharsets.UTF_8));
        return encode(digest);
    }

    private String mutate(String data, String algorithm, byte[] salt, int iterations) {
        byte[] digest = digest(algorithm, iterations, salt, data.getBytes(StandardCharsets.UTF_8));
        String pwd = encode(digest);
        return algorithm + SP + encode(salt) + SP + iterations + SP + pwd;
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
