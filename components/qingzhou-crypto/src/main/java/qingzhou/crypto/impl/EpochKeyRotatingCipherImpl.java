package qingzhou.crypto.impl;

import java.nio.ByteBuffer;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    EpochKeyRotatingCipherImpl(Crypto crypto, byte[] masterKey) {
        this.crypto = crypto;
        this.masterKey = masterKey;
    }

    @Override
    public byte[] encrypt(byte[] data, byte[] context) throws Exception {
        return nowCipher().encrypt(data, context);
    }

    @Override
    public byte[] decrypt(byte[] packet, byte[] context) throws Exception {
        return nowCipher().decrypt(packet, context);
    }

    private ReplayGuardCipher nowCipher() throws Exception {
        return crypto.getReplayGuardCipher(crypto.getCipher(deriveKey()));
    }

    /**
     * 由时间戳所属时段派生时段密钥。返回 24 字符 Base64（16 字节），恰为
     * {@link Crypto#getCipher} 要求的密钥格式；info 采用长度前缀编码防拼接歧义。
     */
    private String deriveKey() {
        ByteBuffer info = ByteBuffer.allocate(8);
        info.putLong(epochIndex());
        return crypto.getBase64Coder().encode(Hkdf.derive(masterKey, info.array(), 16));
    }

    private long epochIndex() {
        long timestampMillis = System.currentTimeMillis();
        return Math.floorDiv(timestampMillis, EPOCH_MILLIS); // TODO 会因为时间差，计算出不同的 index 吗？
    }

    /**
     * RFC 5869 HKDF（HMAC-SHA256）：从高熵根密钥单向派生子密钥。
     * <p>
     * 单向性保证：持有任一子密钥推不出根密钥，不同 info（如不同时段）的子密钥互相独立——
     * 这是"时段密钥泄露不波及其他时段"这一安全属性的密码学基础。
     * <p>
     * 包级私有：仅本包使用。KDF 不产生熵，原语级方法暴露到公共 API 极易被误用（低熵 ikm、info 复用）。
     */
    private static final class Hkdf {
        private static final String HMAC_ALG = "HmacSHA256";
        private static final int HASH_LEN = 32;
        private static final int MAX_LEN = 255 * HASH_LEN; // RFC 5869 输出上限，超出则计数器字节回绕、重复输出同一块
        private static final byte[] ZERO_SALT = new byte[HASH_LEN];

        /**
         * @param ikm    根密钥（须为高熵随机值，如 SecureRandom 产物，不可为口令）
         * @param info   派生上下文（作用域、版本、时段等），不同 info 的派生结果互相独立；null 视为空上下文
         * @param length 输出字节数，取值 [1, 255*32]
         */
        static byte[] derive(byte[] ikm, byte[] info, int length) {
            if (ikm == null || ikm.length == 0) throw new IllegalArgumentException("ikm is required"); // 空根密钥会派生出可预测密钥
            if (length < 1 || length > MAX_LEN) {
                throw new IllegalArgumentException("length out of range [1, " + MAX_LEN + "]: " + length);
            }
            byte[] context = info == null ? new byte[0] : info;
            try {
                // 根密钥已均匀随机，按 RFC 5869 无需 salt；固定全零仅作占位，跨根密钥的隔离由调用方 info 承担
                Mac hmac = Mac.getInstance(HMAC_ALG);
                hmac.init(new SecretKeySpec(ZERO_SALT, HMAC_ALG));
                byte[] prk = hmac.doFinal(ikm);

                byte[] result = new byte[length];
                byte[] t = new byte[0];
                int copied = 0;
                int counter = 1;
                while (copied < length) {
                    hmac.init(new SecretKeySpec(prk, HMAC_ALG));
                    hmac.update(t);
                    hmac.update(context);
                    hmac.update((byte) counter);
                    t = hmac.doFinal();
                    int n = Math.min(HASH_LEN, length - copied);
                    System.arraycopy(t, 0, result, copied, n);
                    copied += n;
                    counter++;
                }
                return result;
            } catch (Exception e) {
                throw new IllegalStateException("hkdf derive failed", e);
            }
        }
    }
}
