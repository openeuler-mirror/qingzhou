package qingzhou.crypto.impl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

import qingzhou.crypto.TotpCipher;

public class TotpCipherImpl implements TotpCipher {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateKey() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public String getCode(String key) throws Exception {
        return generateTOTP(key, System.currentTimeMillis() / 30000);
    }

    @Override
    public boolean verifyCode(String key, String code) throws Exception {
        long timeIndex = System.currentTimeMillis() / 30000;
        return code.equals(generateTOTP(key, timeIndex))
                || code.equals(generateTOTP(key, timeIndex - 1))
                || code.equals(generateTOTP(key, timeIndex + 1));
    }

    private String generateTOTP(String key, long timeIndex) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (timeIndex & 0xFF);
            timeIndex >>= 8;
        }
        SecretKeySpec signKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0xF;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1000000;
        return String.format("%06d", otp);
    }
}