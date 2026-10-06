package qingzhou.crypto;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 请求报文的防重放封装：以时间戳 + 随机数作为 AES-GCM 的附加认证数据（AAD）。
 * <p>
 * 报文格式（registry 与 agent 之间）：
 * <pre>
 * 0      1      2                    10                   26            末尾
 * +------+------+--------------------+--------------------+-------------+
 * | 0xA7 | ver  | timestamp(long BE) | nonce(16B, random) | 密文+标签    |
 * +------+------+--------------------+--------------------+-------------+
 * </pre>
 * AAD = timestamp ‖ nonce，二者都在 GCM 认证范围内，改动任一字节都会令认证失败；
 * 时间戳用于时效判定，随机数用于去重。首字节不是魔数时按旧格式（IV+密文+标签）处理，
 * 以便升级期两端版本不一致时不互相中断。
 */
public final class ReplayGuard {
    /** 新格式魔数：与 AES-GCM 随机 IV 首字节的取值分布无交集，误判概率极低 */
    private static final int MAGIC = 0xA7;
    private static final int VERSION = 1;

    private static final int HEADER_SIZE = 1 + 1 + 8 + 16; // magic+version+timestamp+nonce
    private static final int NONCE_SIZE = 16;

    /** 允许的时钟偏移：超出即拒绝 */
    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000;

    /** 去重键上限：仅用于内存兜底，安全边界是时间窗 */
    private static final int MAX_TRACKED_NONCES = 100_000;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> SEEN_NONCES = new LinkedHashSet<>();

    private ReplayGuard() {
    }

    /** 生成报文内随机数 */
    public static byte[] newNonce() {
        byte[] nonce = new byte[NONCE_SIZE];
        RANDOM.nextBytes(nonce);
        return nonce;
    }

    /** 构造参与 GCM 认证的附加数据 */
    public static byte[] aad(long timestamp, byte[] nonce) {
        return ByteBuffer.allocate(8 + nonce.length).putLong(timestamp).put(nonce).array();
    }

    /** 流式上传：把上传键与分片序号绑定到认证标签，分片无法被重排、重放或跨上传拼接 */
    public static byte[] uploadAad(String uploadKey, long chunkIndex) {
        return ByteBuffer.allocate(8 + 4 + uploadKey.length())
                .putLong(chunkIndex)
                .putInt(uploadKey.hashCode())
                .put(uploadKey.getBytes(StandardCharsets.UTF_8))
                .array();
    }

    /** 封装为新格式报文 */
    public static byte[] wrap(long timestamp, byte[] nonce, byte[] ciphertext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(HEADER_SIZE + ciphertext.length);
        out.write(MAGIC);
        out.write(VERSION);
        byte[] timestampBytes = ByteBuffer.allocate(8).putLong(timestamp).array();
        out.write(timestampBytes, 0, timestampBytes.length);
        out.write(nonce, 0, nonce.length);
        out.write(ciphertext, 0, ciphertext.length);
        return out.toByteArray();
    }

    private static boolean isWrapped(byte[] packet) {
        return packet != null && packet.length > HEADER_SIZE && (packet[0] & 0xFF) == MAGIC;
    }

    /**
     * 解密请求报文并完成防重放校验。
     *
     * @return 明文；报文过期、重放、被篡改或解密失败时返回 null
     */
    public static synchronized byte[] openRequest(Cipher cipher, byte[] packet) {
        if (!isWrapped(packet) || packet[1] != VERSION) return null;

        ByteBuffer header = ByteBuffer.wrap(packet, 2, 8 + NONCE_SIZE);
        long timestamp = header.getLong();
        byte[] nonce = new byte[NONCE_SIZE];
        header.get(nonce);

        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_CLOCK_SKEW_MILLIS) return null;
        if (!rememberNonce(nonce)) return null;

        byte[] ciphertext = new byte[packet.length - HEADER_SIZE];
        System.arraycopy(packet, HEADER_SIZE, ciphertext, 0, ciphertext.length);
        try {
            return cipher.decrypt(ciphertext, aad(timestamp, nonce));
        } catch (Exception e) {
            return null;
        }
    }

    /** 原子登记去重键：并发重放的同一报文只有一个能登记成功 */
    private static boolean rememberNonce(byte[] nonce) {
        String key = Base64.getEncoder().encodeToString(nonce);
        if (!SEEN_NONCES.add(key)) return false;
        if (SEEN_NONCES.size() > MAX_TRACKED_NONCES) {
            int excess = MAX_TRACKED_NONCES / 10;
            Iterator<String> it = SEEN_NONCES.iterator();
            while (it.hasNext() && excess-- > 0) {
                it.next();
                it.remove(); // 按登记顺序淘汰最旧的一批
            }
        }
        return true;
    }
}
