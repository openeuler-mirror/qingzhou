package qingzhou.crypto.impl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import qingzhou.crypto.Cipher;
import qingzhou.crypto.NonceRegistry;
import qingzhou.crypto.ReplayGuardCipher;
import qingzhou.crypto.ReplayGuardException;

/**
 * {@link ReplayGuardCipher} 的默认实现。魔数、版本、时间窗、去重表等细节全部封闭在包内。
 */
final class ReplayGuardCipherImpl implements ReplayGuardCipher {
    /**
     * 新格式魔数：与 AES-GCM 随机 IV 首字节的取值分布无交集，误判概率极低
     */
    private static final int MAGIC = 0xA7;
    private static final int VERSION = 1;

    private static final int NONCE_SIZE = 16;
    private static final int HEADER_SIZE = 1 + 1 + 8 + NONCE_SIZE; // magic+version+timestamp+nonce

    /**
     * 允许的时钟偏移：超出即拒绝
     */
    private static final long MAX_CLOCK_SKEW_MILLIS = 2 * 60 * 1000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Cipher cipher;
    private final NonceRegistry nonceRegistry;

    ReplayGuardCipherImpl(Cipher cipher, NonceRegistry nonceRegistry) {
        this.cipher = cipher;
        this.nonceRegistry = nonceRegistry;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] context) throws Exception {
        if (data == null) return null;

        long timestamp = System.currentTimeMillis();
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);

        byte[] ciphertext = cipher.encrypt(data, aad(timestamp, nonce, context));
        return wrap(timestamp, nonce, ciphertext);
    }

    private byte[] wrap(long timestamp, byte[] nonce, byte[] ciphertext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(HEADER_SIZE + ciphertext.length);
        out.write(MAGIC);
        out.write(VERSION);
        byte[] timestampBytes = ByteBuffer.allocate(8).putLong(timestamp).array();
        out.write(timestampBytes, 0, timestampBytes.length);
        out.write(nonce, 0, nonce.length);
        out.write(ciphertext, 0, ciphertext.length);
        return out.toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] packet, byte[] context) throws Exception {
        if (!isWrapped(packet) || packet[1] != VERSION)
            throw new ReplayGuardException(ReplayGuardException.Reason.INVALID_PACKET, "invalid packet");

        ByteBuffer header = ByteBuffer.wrap(packet, 2, 8 + NONCE_SIZE);
        long timestamp = header.getLong();
        byte[] nonce = new byte[NONCE_SIZE];
        header.get(nonce);

        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MILLIS)
            throw new ReplayGuardException(ReplayGuardException.Reason.TIMESTAMP_OUT_OF_WINDOW, "timestamp out of window");

        byte[] ciphertext = new byte[packet.length - HEADER_SIZE];
        System.arraycopy(packet, HEADER_SIZE, ciphertext, 0, ciphertext.length);

        // 先解密后登记：解密失败（如 context 不匹配）不得烧掉 nonce，否则攻击者可抢先令合法报文被判为重放
        byte[] plaintext = cipher.decrypt(ciphertext, aad(timestamp, nonce, context));
        if (!registerIfAbsent(nonce, timestamp)) {
            throw new ReplayGuardException(ReplayGuardException.Reason.REPLAY, "replay detected");
        }
        return plaintext;
    }

    private static boolean isWrapped(byte[] packet) {
        return packet != null && packet.length > HEADER_SIZE && (packet[0] & 0xFF) == MAGIC;
    }

    private byte[] aad(long timestamp, byte[] nonce, byte[] context) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + nonce.length + (context == null ? 0 : context.length));
        buffer.putLong(timestamp).put(nonce);
        if (context != null) buffer.put(context);
        return buffer.array();
    }

    // 先解密后登记：解密失败（如 context 不匹配）不得烧掉 nonce，否则攻击者可抢先令合法报文被判为重放。
    // TTL 取时间窗两倍：过期键先回收，仍超容量时由登记器拒绝而非丢弃窗口内记录
    private boolean registerIfAbsent(byte[] nonce, long timestamp) {
        String key = Base64.getEncoder().encodeToString(nonce);
        return nonceRegistry.registerIfAbsent(key, timestamp, MAX_CLOCK_SKEW_MILLIS * 2);
    }
}
