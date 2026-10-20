package qingzhou.crypto.impl;

import qingzhou.crypto.Crypto;
import qingzhou.crypto.EpochPacketCipher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@link EpochPacketCipher} 的默认实现：绑定根密钥与作用域，时段长度、报文头布局等细节封闭在包内。
 */
final class EpochPacketCipherImpl implements EpochPacketCipher {
    /**
     * 时段长度：单时段密钥泄露的爆炸半径粒度。派生开销可忽略，取 10 分钟把窗口压到
     * 与一次运维处置相当的量级；重放去重容量由 ReplayGuard 的时间窗决定，与本参数无关。
     */
    private static final long EPOCH_MILLIS = 10 * 60 * 1000L;

    private final Crypto crypto;
    private final byte[] master;
    private final String scopeId;

    EpochPacketCipherImpl(Crypto crypto, byte[] master, String scopeId) {
        this.crypto = crypto;
        this.master = master;
        this.scopeId = scopeId;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] context) throws Exception {
        return crypto.getReplayGuardCipher(crypto.getCipher(deriveKey())).encrypt(data, context);
    }

    @Override
    public byte[] decrypt(byte[] packet, byte[] context) throws Exception {
        return crypto.getReplayGuardCipher(crypto.getCipher(deriveKey())).decrypt(packet, context);
    }

    /**
     * 由时间戳所属时段派生时段密钥。返回 24 字符 Base64（16 字节），恰为
     * {@link Crypto#getCipher} 要求的密钥格式；info 采用长度前缀编码防拼接歧义。
     */
    private String deriveKey() {
        long timestampMillis = System.currentTimeMillis(); // 可能与 ReplayGuardCipher.encrypt 存在 极小的 时间差，但可以被密钥生成算法容错
        byte[] scope = scopeId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer info = ByteBuffer.allocate(4 + scope.length + 8);
        info.putInt(scope.length).put(scope).putLong(epochIndex(timestampMillis));
        return Base64.getEncoder().encodeToString(Hkdf.derive(master, info.array(), 16));
    }

    private static long epochIndex(long timestampMillis) {
        return Math.floorDiv(timestampMillis, EPOCH_MILLIS);
    }
}
