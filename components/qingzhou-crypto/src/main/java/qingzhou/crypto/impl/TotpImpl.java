package qingzhou.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import qingzhou.crypto.Base16Coder;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.Totp;

class TotpImpl implements Totp {
    // RFC 6238 默认步长 30 秒，主流验证器 App 亦固定此值，不可调整
    static final long STEP_MILLIS = 30_000L; // 包级可见：测试复用，避免硬编码步长

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    private final Base16Coder base16Coder;
    private final Base32Coder base32Coder;
    private final Map<String, AtomicLong> consumedWindows = new ConcurrentHashMap<>(); // key -> 已消费的最大窗口序号

    TotpImpl(Base16Coder base16Coder, Base32Coder base32Coder) {
        this.base16Coder = base16Coder;
        this.base32Coder = base32Coder;
    }

    @Override
    public String generateKey() {
        // RFC 4226 建议共享密钥不少于 160 位（20 字节），取 32 字节以留足余量
        byte[] salt = new byte[32];
        RANDOM.nextBytes(salt);
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
        return matchWindow(key, code) >= 0;
    }

    @Override
    public boolean verifyCodeWithReplayGuard(String key, String code) throws Exception {
        long window = matchWindow(key, code);
        if (window < 0) return false;
        AtomicLong consumed = consumedWindows.computeIfAbsent(key, k -> new AtomicLong(-1));
        // 仅接受严格递增的窗口序号；CAS 保证并发下同一窗口只被消费一次
        for (long max = consumed.get(); window > max; max = consumed.get()) {
            if (consumed.compareAndSet(max, window)) return true;
        }
        return false;
    }

    // 命中前/当前/后一个时间窗口时返回该窗口序号，-1 表示不匹配。包级可见：测试复用
    long matchWindow(String key, String code) throws Exception {
        if (key == null || key.isEmpty() || code == null || code.isEmpty()) return -1;
        long step = System.currentTimeMillis() / STEP_MILLIS; // 基准只取一次，否则校验途中跨窗会漏检真正的当前窗口
        if (isEqual(code, getCode(key, step))) return step;
        if (isEqual(code, getCode(key, step - 1))) return step - 1; // 提交延迟只会把口令推向前序窗口
        if (isEqual(code, getCode(key, step + 1))) return step + 1; // 覆盖服务端时钟偏慢的情况
        return -1;
    }

    // 常量时间比较，防止时序侧信道推算口令
    private static boolean isEqual(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
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
