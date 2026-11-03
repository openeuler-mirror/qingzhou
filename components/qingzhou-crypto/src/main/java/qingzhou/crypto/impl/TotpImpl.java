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

import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.Totp;

class TotpImpl implements Totp {
    // RFC 6238 默认步长 30 秒，主流验证器 App 亦固定此值，不可调整
    private static final long STEP_MILLIS = 30_000L;

    private final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, AtomicLong> consumedWindows = new ConcurrentHashMap<>(); // key -> 已消费的最大窗口序号

    private final Base32Coder base32Coder;

    TotpImpl(Base32Coder base32Coder) {
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

    String getCode(String key, long window) throws Exception {
        return generateTOTP(base32Coder.decode(key), window);
    }

    @Override
    public boolean verifyCodeWithReplayGuard(String key, String code) throws Exception {
        long window = matchWindow(key, code);
        if (window < 0) return false;
        AtomicLong consumed = consumedWindows.computeIfAbsent(key, k -> new AtomicLong(-1));
        // 旧值小于当前窗口即为首次消费；相等/更早即重放。getAndAccumulate 原子
        return window > consumed.getAndAccumulate(window, Math::max);
    }

    // 命中前/当前/后一个时间窗口时返回该窗口序号，-1 表示不匹配
    private long matchWindow(String key, String code) throws Exception {
        if (key == null || key.isEmpty() || code == null || code.isEmpty()) return -1;
        byte[] secret = base32Coder.decode(key);
        long window = System.currentTimeMillis() / STEP_MILLIS; // 基准只取一次，避免校验途中跨窗漏检当前窗口
        // 三个窗口都计算并比较（不早退），执行路径恒定，避免泄露命中的窗口位置
        long matched = -1;
        for (long w = window - 1; w <= window + 1; w++) {
            if (isEqual(code, generateTOTP(secret, w))) matched = w;
        }
        return matched;
    }

    // 常量时间比较，防止时序侧信道推算口令
    private boolean isEqual(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    // RFC 4226（HOTP）：时间计数器编码为 8 字节大端后参与 HMAC-SHA1
    private String generateTOTP(byte[] key, long window) throws Exception {
        byte[] counter = new byte[8];
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) window;
            window >>>= 8;
        }
        return computeCode(hMac(key, counter));
    }

    // RFC 4226 动态口令截断：取哈希末字节低 4 位为偏移，取 4 字节屏蔽符号位后对 10^6 取模并补零至 6 位
    private String computeCode(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        return String.format("%06d", binary % 1_000_000);
    }

    // HmacSHA1 固定：RFC 6238 默认算法；otpauth:// 未声明 algorithm 时各验证器一律按 SHA1 计算
    private byte[] hMac(byte[] key, byte[] text) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(key, "RAW"));
        return hmac.doFinal(text);
    }
}
