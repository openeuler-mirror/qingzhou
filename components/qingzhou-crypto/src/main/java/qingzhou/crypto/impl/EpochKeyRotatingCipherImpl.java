package qingzhou.crypto.impl;

import java.nio.ByteBuffer;

import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochKeyRotatingCipher;
import qingzhou.crypto.ReplayGuardCipher;

/**
 * {@link EpochKeyRotatingCipher} 的默认实现：绑定根密钥与作用域，时段长度、报文头布局等细节封闭在包内。
 */
final class EpochKeyRotatingCipherImpl implements EpochKeyRotatingCipher {
    /**
     * 时段长度：单时段密钥泄露的爆炸半径粒度。派生开销可忽略，取 10 分钟把窗口压到
     * 与一次运维处置相当的量级；重放去重容量由 ReplayGuard 的时间窗决定，与本参数无关。
     */
    private static final long EPOCH_MILLIS = 10 * 60 * 1000L;

    private final Crypto crypto;
    private final byte[] masterKey;

    // 时段密码器单槽缓存：时段 10 分钟才变，合法流量恒落在当前时段；
    // 不用无界 Map——解密按报文自带时间戳选钥，伪造报文可用任意时间戳制造大量时段导致内存膨胀
    private volatile CipherSlot cipherSlot;

    private static final class CipherSlot {
        final long epoch;
        final ReplayGuardCipher cipher;

        CipherSlot(long epoch, ReplayGuardCipher cipher) {
            this.epoch = epoch;
            this.cipher = cipher;
        }
    }

    EpochKeyRotatingCipherImpl(Crypto crypto, byte[] masterKey) {
        this.crypto = crypto;
        this.masterKey = masterKey;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] context) throws Exception {
        long epoch = epochIndex(System.currentTimeMillis());
        byte[] packet = cipherFor(epoch).encrypt(data, context);
        // 派生密钥期间恰跨时段边界：报文时间戳会落到下一时段，接收方按时间戳选钥必然解不开，按新时段重加密一次
        if (epochIndex(ReplayGuardCipherImpl.timestampOf(packet)) != epoch) {
            packet = cipherFor(epochIndex(System.currentTimeMillis())).encrypt(data, context);
        }
        return packet;
    }

    @Override
    public byte[] decrypt(byte[] packet, byte[] context) throws Exception {
        // 按报文自带时段选钥：用本机当前时段会在双方跨时段边界时必然解密失败
        return cipherFor(epochIndex(ReplayGuardCipherImpl.timestampOf(packet))).decrypt(packet, context);
    }

    private ReplayGuardCipher cipherFor(long epoch) throws Exception {
        CipherSlot slot = cipherSlot;
        if (slot != null && slot.epoch == epoch) return slot.cipher;
        // 不可变槽整体经 volatile 发布：避免 epoch 与 cipher 分两字段写时被并发读到错配组合
        CipherSlot fresh = new CipherSlot(epoch, crypto.getReplayGuardCipher(crypto.getCipher(deriveKey(epoch))));
        cipherSlot = fresh;
        return fresh.cipher;
    }

    /**
     * 由时段索引派生时段密钥，输出 32 字节（AES-256）：Grover 攻击下仍保 128 位安全，满足后量子强度；info 为 8 字节时段索引。
     */
    private byte[] deriveKey(long epoch) {
        ByteBuffer info = ByteBuffer.allocate(8);
        info.putLong(epoch);
        return Hkdf.derive(masterKey, info.array(), 32);
    }

    private long epochIndex(long timestampMillis) {
        return Math.floorDiv(timestampMillis, EPOCH_MILLIS);
    }
}
