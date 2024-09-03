package qingzhou.crypto.impl;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.TotpCipher;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

class TotpCipherImpl implements TotpCipher {
    private final Base16Coder base16Coder;
    private final Base32Coder base32Coder;
    private final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    TotpCipherImpl(Base16Coder base16Coder, Base32Coder base32Coder) {
        this.base16Coder = base16Coder;
        this.base32Coder = base32Coder;
    }

    @Override
    public String generateKey() {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        return base32Coder.encode(salt);
    }

    @Override
    public String getCode(String key) throws Exception {
        long currentTime = System.currentTimeMillis() / 1000L;
        long t = currentTime / 30;
        String time = Long.toHexString(t).toUpperCase();

        return generateTOTP(base32Coder.decode(key), time);
    }

    @Override
    public boolean verifyCode(String key, String code) throws Exception {
        return getCode(key).equals(code);
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
    private String generateTOTP(byte[] key, String time) throws Exception {
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        StringBuilder timeBuilder = new StringBuilder(time);
        while (timeBuilder.length() < 16) {
            timeBuilder.insert(0, "0");
        }
        time = timeBuilder.toString();

        byte[] msg = base16Coder.decode(time);
        byte[] hash = hMac(key, msg);

        return computeCode(hash);
    }

    private String computeCode(byte[] hash) {
        int lenOfDigits = 6;

        int offset = hash[hash.length - 1] & 0xf;
        int binary =
                ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);
        int otp = binary % DIGITS_POWER[lenOfDigits];

        StringBuilder result = new StringBuilder(Integer.toString(otp));
        while (result.length() < lenOfDigits) {
            result.insert(0, "0");
        }
        return result.toString();
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
    private byte[] hMac(byte[] key, byte[] text) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(key, "RAW"));
        return hmac.doFinal(text);
    }
}
