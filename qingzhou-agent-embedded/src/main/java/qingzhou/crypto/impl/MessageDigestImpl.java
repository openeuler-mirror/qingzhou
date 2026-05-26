package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import qingzhou.crypto.MessageDigest;

public class MessageDigestImpl implements MessageDigest {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String digest(String text, String alg, int saltLength, int iterations) {
        try {
            byte[] salt = new byte[saltLength];
            random.nextBytes(salt);
            byte[] hash = hash(text.getBytes(StandardCharsets.UTF_8), salt, iterations, alg);
            byte[] result = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(hash, 0, result, salt.length, hash.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Digest failed", e);
        }
    }

    @Override
    public boolean matches(String text, String digest) {
        try {
            byte[] allBytes = Base64.getDecoder().decode(digest);
            // Salt length is determined from the algorithm
            if (digest.length() < 44) return false; // minimum base64 length
            byte[] salt = new byte[16]; // default salt length
            System.arraycopy(allBytes, 0, salt, 0, salt.length);
            byte[] hash = new byte[allBytes.length - salt.length];
            System.arraycopy(allBytes, salt.length, hash, 0, hash.length);
            byte[] computed = hash(text.getBytes(StandardCharsets.UTF_8), salt, 1000, "SHA-256");
            return java.security.MessageDigest.isEqual(hash, computed);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String md5(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("MD5 failed", e);
        }
    }

    @Override
    public String sha256(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private byte[] hash(byte[] data, byte[] salt, int iterations, String algorithm) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance(algorithm);
        md.update(salt);
        byte[] hash = md.digest(data);
        for (int i = 1; i < iterations; i++) {
            md.reset();
            hash = md.digest(hash);
        }
        return hash;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}