package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.MessageDigest;

class MessageDigestImpl implements MessageDigest {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SALT_SEPARATOR = "$";
    // 迭代数上限：来自被校验串，不设上界会被恶意摘要用超大迭代数拖垮 CPU（CPU DoS）
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final int MAX_PASSWORD_CHARS = 1024; // 与登录入口上限一致，避免超长口令拖垮 PBKDF2
    private static final int MAX_SALT_BYTES = 64; // 正常盐 16 字节；上限防止异常摘要触发超长 PBKDF2
    // 算法白名单：外部可指定算法生成口令摘要，放行 MD5/SHA-1 等弱算法会被用于弱口令存储
    private static final java.util.Set<String> ALLOWED_ALGORITHMS = new java.util.HashSet<>(java.util.Arrays.asList(
            "SHA-256", "SHA-384", "SHA-512", "PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA512"));

    private final Base16Coder base16Coder;

    MessageDigestImpl(Base16Coder base16Coder) {
        this.base16Coder = base16Coder;
    }

    @Override
    public String digest(String text) {
        return digest(text, "PBKDF2WithHmacSHA256", 16, 200000);
    }

    @Override
    public String digest(String text, String algorithm, int saltLength, int iterations) {
        if (!ALLOWED_ALGORITHMS.contains(algorithm)) {
            throw new IllegalArgumentException("digest algorithm not allowed: " + algorithm);
        }
        if (saltLength < 0 || saltLength > MAX_SALT_BYTES) {
            throw new IllegalArgumentException("saltLength must be within 0~" + MAX_SALT_BYTES + ": " + saltLength);
        }
        if (iterations < 1 || iterations > MAX_ITERATIONS) { // 与校验侧同一上界：生成侧同样不得被超大迭代数拖垮
            throw new IllegalArgumentException("iterations must be within 1~" + MAX_ITERATIONS + ": " + iterations);
        }
        byte[] salt = null;
        if (saltLength > 0) {
            salt = new byte[saltLength];
            RANDOM.nextBytes(salt);
        }
        return mutate(text, algorithm, salt, iterations);
    }

    @Override
    public boolean matches(String text, String msgDigest) {
        if (text == null || msgDigest == null) {
            return false;
        }

        if (text.length() > MAX_PASSWORD_CHARS) return false;

        try {
            String[] splitPwd = msgDigest.split("\\" + SALT_SEPARATOR);
            if (splitPwd.length != 4) return false; // 格式非法的摘要直接判定不匹配

            String algorithm = splitPwd[0];
            if (!ALLOWED_ALGORITHMS.contains(algorithm)) return false; // 与 digest 同一白名单：MD5/SHA-1 等弱算法摘要不接受校验
            byte[] salt = decode(splitPwd[1]);
            if (salt.length > MAX_SALT_BYTES) return false;
            int iterations = Integer.parseInt(splitPwd[2]);
            if (iterations < 1 || iterations > MAX_ITERATIONS) return false;
            String digest = mutate(text, algorithm, salt, iterations);
            // 常量时间比较，防止时序侧信道推算摘要
            return java.security.MessageDigest.isEqual(digest.getBytes(StandardCharsets.UTF_8),
                    msgDigest.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 段数合法但内容非法（坏盐/坏迭代数/未知算法）按不匹配处理：登录应回 401 而非 500
            return false;
        }
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
        byte[] digest;
        if (algorithm.startsWith("PBKDF2")) {
            // 口令摘要使用 PBKDF2 而非简单迭代哈希：带盐、高迭代次数，抗离线爆破
            digest = pbkdf2(algorithm, iterations, salt, data);
        } else {
            digest = digest(algorithm, iterations, salt,
                    data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
        }
        String pwd = encode(digest);
        return algorithm + SALT_SEPARATOR + encode(salt) + SALT_SEPARATOR + iterations + SALT_SEPARATOR + pwd;
    }

    private byte[] pbkdf2(String algorithm, int iterations, byte[] salt, String password) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
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
