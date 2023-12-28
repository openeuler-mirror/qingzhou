package qingzhou.console.sec;

import qingzhou.console.util.HexUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

public class Digest {
    private static final Random random = new SecureRandom();

    private static final String SP = "$";

    public static String mutate(String inputCredentials, String algorithm, int saltLength, int iterations) {
        byte[] salt = new byte[saltLength];
        random.nextBytes(salt);
        return mutate(inputCredentials, algorithm, salt, iterations);
    }

    public static boolean matches(String inputCredentials, String storedCredentials) {
        if (inputCredentials == null || storedCredentials == null) {
            return false;
        }

        String[] splitPwd = splitPwd(storedCredentials);
        String algorithm = splitPwd[3];
        int iterations = Integer.parseInt(splitPwd[1]);
        byte[] salt = decode(splitPwd[0]);
        String mutate = mutate(inputCredentials, algorithm, salt, iterations);
        return Objects.equals(mutate, storedCredentials);
    }

    private static String mutate(String inputCredentials, String algorithm, byte[] salt, int iterations) {
        byte[] digest = digest(algorithm, iterations, salt, inputCredentials.getBytes(StandardCharsets.UTF_8));
        String pwd = encode(digest);
        return encode(salt) + SP + iterations + SP + pwd + SP + algorithm;
    }

    private static byte[] decode(String encode) {
        return HexUtil.hexToBytes(encode);
    }

    private static String encode(byte[] bytes) {
        return HexUtil.bytesToHex(bytes);
    }

    private static String[] splitPwd(String storedCredentials) {
        String[] pwdArray = new String[4];
        int lastIndexOf = storedCredentials.lastIndexOf(SP);

        String digestAlg = storedCredentials.substring(lastIndexOf + 1);
        pwdArray[pwdArray.length - 1] = digestAlg;

        storedCredentials = storedCredentials.substring(0, lastIndexOf);
        String[] oldPwdDigestStyle = storedCredentials.split("\\" + SP);
        System.arraycopy(oldPwdDigestStyle, 0, pwdArray, 0, pwdArray.length - 1);

        return pwdArray;
    }

    private static byte[] digest(String algorithm, int iterations, byte[]... input) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        // Round 1
        for (byte[] bytes : input) {
            md.update(bytes);
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

    private Digest() {
    }
}
