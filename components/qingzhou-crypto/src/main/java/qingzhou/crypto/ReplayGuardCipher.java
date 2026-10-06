package qingzhou.crypto;

/**
 * 请求报文的防重放封装：以时间戳 + 随机数作为 AES-GCM 的附加认证数据（AAD）。
 * <p>
 * 报文格式：
 * <pre>
 * 0      1      2                    10                   26          末尾
 * +------+------+--------------------+--------------------+-----------+
 * | 0xA7 | ver  | timestamp(long BE) | nonce(16B, random) | 密文+标签   |
 * +------+------+--------------------+--------------------+-----------+
 * </pre>
 * AAD = timestamp ‖ nonce ‖ context。时间戳用于时效判定（允许有限的时钟偏移），随机数用于去重，
 * 三者都在 GCM 认证范围内，改动任一字节都会令认证失败。
 * <p>
 * 去重表由实现全局共享：不同实例（不同密钥）登记到同一张表，容量仅作内存兜底，安全边界是时间窗。
 */
public interface ReplayGuardCipher {
    /**
     * 加密并封装为防重放报文。
     *
     * @param context 额外绑定的上下文字节（如上传键），可为 null；须与 {@link #decrypt} 时一致
     */
    byte[] encrypt(byte[] data, int off, int len, byte[] context) throws Exception;

    /**
     * 校验时间窗与去重并解密。
     * <p>
     * 报文无效（格式、版本、时间窗）或重放时抛 {@link IllegalArgumentException}；
     * 解密失败抛 {@link Exception}。nonce 仅在解密成功后登记，避免被无效请求预先烧掉。
     *
     * @param context 须与 {@link #encrypt} 时一致
     * @return 明文
     */
    byte[] decrypt(byte[] packet, byte[] context) throws Exception;
}
