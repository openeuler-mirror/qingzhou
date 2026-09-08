package qingzhou.crypto.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.TotpCipher;

class TotpCipherImpl implements TotpCipher {
    // RFC 6238 默认步长 30 秒，主流验证器 App 亦固定此值，不可调整
    private static final long STEP_MILLIS = 30_000L;

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
        return getCode(key, System.currentTimeMillis() / STEP_MILLIS);
    }

    /**
     * @param step 绝对时间窗口序号，而非相对偏移量。
     */
    String getCode(String key, long step) throws Exception {
        return generateTOTP(base32Coder.decode(key), Long.toHexString(step).toUpperCase());
    }

    @Override
    public boolean verifyCode(String key, String code) throws Exception {
        if (key == null || key.isEmpty() || code == null || code.isEmpty()) return false;
        long step = System.currentTimeMillis() / STEP_MILLIS; // 基准只取一次，否则校验途中跨窗会漏检真正的当前窗口
        return code.equals(getCode(key, step))
                || code.equals(getCode(key, step - 1)) // 提交延迟只会把口令推向前序窗口
                || code.equals(getCode(key, step + 1)); // 覆盖服务端时钟偏慢的情况
    }

    /**
     * 按 RFC 4226（HOTP）计算口令，时间计数器左补零至 16 位十六进制（即 8 字节）后参与 HMAC。
     */
    private String generateTOTP(byte[] key, String time) throws Exception {
        StringBuilder timeBuilder = new StringBuilder(time);
        while (timeBuilder.length() < 16) {
            timeBuilder.insert(0, "0");
        }
        time = timeBuilder.toString();

        byte[] msg = base16Coder.decode(time);
        byte[] hash = hMac(key, msg);

        return computeCode(hash);
    }

    /**
     * RFC 4226 定义的动态口令截断：取哈希最后一个字节的低 4 位作为偏移量，
     * 从该位置取 4 字节、屏蔽符号位后对 10 的位数次幂取模，左侧补零至固定位数。
     */
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
     * 固定使用 HmacSHA1：RFC 6238 规定 TOTP 默认算法为 SHA1，
     * 而 otpauth:// URI 未声明 algorithm 时，Google Authenticator、Microsoft Authenticator、1Password、Authy 一律按 SHA1 计算。
     * 改用 SHA256 将导致这些验证器生成的口令全部校验失败。
     */
    private byte[] hMac(byte[] key, byte[] text) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(key, "RAW"));
        return hmac.doFinal(text);
    }
}
