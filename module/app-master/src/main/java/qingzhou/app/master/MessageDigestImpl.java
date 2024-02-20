package qingzhou.app.master;

import qingzhou.framework.util.HexUtil;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MessageDigestImpl {
    public static void main(String[] args) {
        String digest = new MessageDigestImpl().digest("qingzhou123.com", "SHA-256", 2, 2);
        System.out.println(digest);
    }

    private final String SP = "$";

    public String digest(String text, String algorithm, int saltLength, int iterations) {
        byte[] salt = new byte[saltLength];
        Random random = ThreadLocalRandom.current();//TODO 临时解决新创建用户无法登录问题
        random.nextBytes(salt);
        return digest(text, algorithm, salt, iterations);
    }

    public boolean matches(String text, String msgDigest) {
        if (text == null || msgDigest == null) {
            return false;
        }

        String[] splitPwd = splitPwd(msgDigest);
        String algorithm = splitPwd[3];
        int iterations = Integer.parseInt(splitPwd[1]);
        byte[] salt = decode(splitPwd[0]);
        String digest = digest(text, algorithm, salt, iterations);
        return Objects.equals(digest, msgDigest);
    }

    private String digest(String inputCredentials, String algorithm, byte[] salt, int iterations) {
        byte[] digest = digest(algorithm, iterations, salt, inputCredentials.getBytes(StandardCharsets.UTF_8));
        String pwd = encode(digest);
        return encode(salt) + SP + iterations + SP + pwd + SP + algorithm;
    }

    private byte[] decode(String encode) {
        return HexUtil.hexToBytes(encode);
    }

    private String encode(byte[] bytes) {
        return HexUtil.bytesToHex(bytes);
    }

    private String[] splitPwd(String storedCredentials) {
        String[] pwdArray = new String[4];
        int lastIndexOf = storedCredentials.lastIndexOf(SP);

        String digestAlg = storedCredentials.substring(lastIndexOf + 1);
        pwdArray[pwdArray.length - 1] = digestAlg;

        storedCredentials = storedCredentials.substring(0, lastIndexOf);
        String[] oldPwdDigestStyle = storedCredentials.split("\\" + SP);
        System.arraycopy(oldPwdDigestStyle, 0, pwdArray, 0, pwdArray.length - 1);

        return pwdArray;
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
}
