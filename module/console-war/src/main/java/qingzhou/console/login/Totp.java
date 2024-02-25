package qingzhou.console.login;

import qingzhou.console.util.HexUtil;
import qingzhou.framework.util.Base32Util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 *
 */
public class Totp {
    private static final int lenOfDigits = 6;
    private static final String crypto = "HmacSHA1";// "HmacSHA256"、"HmacSHA512"
    private static final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private Totp() {
    }

    public static void main(String[] args) throws Exception {
        String secretBase32 = getRandomSecretBase32(16);
        System.out.println("密钥：" + secretBase32);
        String totpString = generateTotpString("test", secretBase32);
        System.out.println("协议：" + totpString);
        String dynamicCode = generate(secretBase32);
        System.out.println("生成动态口令：" + dynamicCode);
        boolean verify = verify(secretBase32, dynamicCode);
        System.out.println("校验通过：" + verify);
    }


    /**
     * 生成base32编码的随机密钥
     */
    public static String getRandomSecretBase32(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length / 2];
        random.nextBytes(salt);
        return Base32Util.encode(salt);
    }

    /**
     * 生成totp协议字符串
     */
    public static String generateTotpString(String userInfo, String secretBase32) {
        return "otpauth://totp/" + userInfo + "?secret=" + secretBase32;
    }

    /**
     * 验证动态口令是否正确
     * <p>
     * secretBase32 密钥
     * code         待验证的动态口令
     */
    public static boolean verify(String secretBase32, String code) throws Exception {
        return generate(secretBase32).equals(code);
    }

    /**
     * 根据密钥生成动态口令
     * <p>
     * secretBase32 base32编码格式的密钥
     */
    public static String generate(String secretBase32) throws Exception {

        String secretHex = HexUtil.bytesToHex(Base32Util.decode(secretBase32));

        long X = 30;

        StringBuilder steps;
        long currentTime = System.currentTimeMillis() / 1000L;
        long t = currentTime / X;
        steps = new StringBuilder(Long.toHexString(t).toUpperCase());
        while (steps.length() < 16) {
            steps.insert(0, "0");
        }

        return generateTOTP(secretHex, steps.toString(), lenOfDigits, crypto);
    }

    /**
     * This method generates a TOTP value for the given
     * set of parameters.
     * <p>
     * key:          the shared secret, HEX encoded
     * time:         a value that reflects a time
     * returnDigits: number of digits to return
     * crypto:       the crypto function to use
     * return: a numeric String in base 10 that includes truncationDigits digits
     */
    public static String generateTOTP(String key,
                                      String time,
                                      int lenOfDigits,
                                      String crypto) {
        StringBuilder result;

        // Using the counter
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        StringBuilder timeBuilder = new StringBuilder(time);
        while (timeBuilder.length() < 16) {
            timeBuilder.insert(0, "0");
        }
        time = timeBuilder.toString();

        // Get the HEX in a Byte[]
        byte[] msg = hexStr2Bytes(time);
        byte[] k = hexStr2Bytes(key);

        byte[] hash = hmac_sha(crypto, k, msg);

        // put selected bytes into result int
        int offset = hash[hash.length - 1] & 0xf;

        int binary =
                ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);

        int otp = binary % DIGITS_POWER[lenOfDigits];

        result = new StringBuilder(Integer.toString(otp));
        while (result.length() < lenOfDigits) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    /**
     * This method converts a HEX string to Byte[]
     * <p>
     * hex: the HEX string
     * return: a byte array
     */
    private static byte[] hexStr2Bytes(String hex) {
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

        // Copy all the REAL bytes, not the "first"
        byte[] ret = new byte[bArray.length - 1];
        System.arraycopy(bArray, 1, ret, 0, ret.length);
        return ret;
    }

    /**
     * This method uses the JCE to provide the crypto algorithm.
     * HMAC computes a Hashed Message Authentication Code with the
     * crypto hash algorithm as a parameter.
     * <p>
     * crypto:   the crypto algorithm (HmacSHA1, HmacSHA256,
     * HmacSHA512)
     * keyBytes: the bytes to use for the HMAC key
     * text:     the message or text to be authenticated
     */
    private static byte[] hmac_sha(String crypto, byte[] keyBytes,
                                   byte[] text) {
        try {
            Mac hmac;
            hmac = Mac.getInstance(crypto);
            SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
            hmac.init(macKey);
            return hmac.doFinal(text);
        } catch (GeneralSecurityException gse) {
            throw new UndeclaredThrowableException(gse);
        }
    }
}
