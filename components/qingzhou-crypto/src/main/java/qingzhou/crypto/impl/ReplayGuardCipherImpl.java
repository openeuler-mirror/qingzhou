package qingzhou.crypto.impl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Cipher;
import qingzhou.crypto.ReplayGuardCipher;

/**
 * {@link ReplayGuardCipher} 的默认实现。魔数、版本、时间窗、去重表等细节全部封闭在包内。
 */
final class ReplayGuardCipherImpl implements ReplayGuardCipher {
    /**
     * 新格式魔数：AES-GCM 输出首字节随机，仍可能恰好为 0xA7（叠加版本字节后误判概率约 1/65536），
     * 误判报文会在版本/时间窗/GCM 认证校验中被拒绝，不构成安全或正确性问题
     */
    private static final int MAGIC = 0xA7;
    private static final int VERSION = 1;

    private static final int NONCE_SIZE = 16;
    private static final int TIMESTAMP_OFFSET = 2; // magic + version
    private static final int TIMESTAMP_END = TIMESTAMP_OFFSET + 8;
    private static final int HEADER_SIZE = TIMESTAMP_END + NONCE_SIZE; // magic+version+timestamp+nonce

    /**
     * 允许的时钟偏移：超出即拒绝
     */
    private static final long MAX_CLOCK_SKEW_MILLIS = 2 * 60 * 1000;

    private static final SecureRandom RANDOM = new SecureRandom();

    // 共享登记器：同一 JVM 内所有方向的报文共用一份去重状态
    private static final NonceRegistry nonceRegistry = new NonceRegistry();

    private final Cipher cipher;
    private final Base64Coder base64Coder;

    ReplayGuardCipherImpl(Cipher cipher, Base64Coder base64Coder) {
        this.cipher = cipher;
        this.base64Coder = base64Coder;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] context) throws Exception {
        long timestamp = System.currentTimeMillis();
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);
        byte[] ciphertext = cipher.encrypt(data, aad(timestamp, nonce, context));
        ByteBuffer packet = ByteBuffer.allocate(HEADER_SIZE + ciphertext.length);
        packet.put((byte) MAGIC).put((byte) VERSION).putLong(timestamp).put(nonce).put(ciphertext);
        return packet.array();
    }

    @Override
    public byte[] decrypt(byte[] packet, byte[] context) throws Exception {
        if (!isWrapped(packet))
            throw new IllegalArgumentException("invalid packet");

        long timestamp = timestampOf(packet);
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MILLIS)
            throw new IllegalArgumentException("timestamp out of window");

        byte[] ciphertext = Arrays.copyOfRange(packet, HEADER_SIZE, packet.length);

        byte[] nonce = new byte[NONCE_SIZE];
        ByteBuffer.wrap(packet, TIMESTAMP_END, NONCE_SIZE).get(nonce);

        byte[] plaintext = cipher.decrypt(ciphertext, aad(timestamp, nonce, context));
        // 放于 cipher.decrypt 之后，先解密后登记：解密失败（如 context 不匹配）不得烧掉 nonce，否则攻击者可抢先令合法报文被判为重放
        if (!registerIfAbsent(nonce)) {
            throw new IllegalArgumentException("replay detected");
        }
        return plaintext;
    }

    // 魔数与版本共同标识本协议报文：任一不符即按无效报文拒绝，对外统一报错不给区分信息
    private boolean isWrapped(byte[] packet) {
        return packet != null && packet.length > HEADER_SIZE
                && (packet[0] & 0xFF) == MAGIC && packet[1] == VERSION;
    }

    /**
     * 报文自带时间戳（受 GCM AAD 完整性保护）；供同包的时段密码器按时间戳选钥。
     * 长度不足按无效报文拒绝，避免越界读取。
     */
    private long timestampOf(byte[] packet) {
        if (packet == null || packet.length < TIMESTAMP_END) {
            throw new IllegalArgumentException("packet too short");
        }
        return ByteBuffer.wrap(packet, TIMESTAMP_OFFSET, 8).getLong();
    }

    private byte[] aad(long timestamp, byte[] nonce, byte[] context) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + nonce.length + (context == null ? 0 : context.length));
        buffer.putLong(timestamp).put(nonce);
        if (context != null) buffer.put(context);
        return buffer.array();
    }

    // 先解密后登记：解密失败（如 context 不匹配）不得烧掉 nonce，否则攻击者可抢先令合法报文被判为重放。
    // TTL 取时间窗两倍：自登记时刻起覆盖该报文在时间窗内的全部可受理时段
    private boolean registerIfAbsent(byte[] nonce) {
        String key = base64Coder.encode(nonce);
        return nonceRegistry.registerIfAbsent(key, MAX_CLOCK_SKEW_MILLIS * 2);
    }
}
