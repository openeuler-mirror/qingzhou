package qingzhou.crypto;

/**
 * 防重放的去重登记器：按 nonce 去重，与具体加密方式无关。
 * <p>
 * 供 {@link ReplayGuardCipher}（AES-GCM 报文）与注册协议（RSA 引导报文）共用：
 * 两者时间窗不同，故 TTL 由调用方按自身窗口给出（通常取窗口的两倍）。
 */
public interface NonceRegistry {
    /**
     * 原子登记 nonce。
     *
     * @param nonce     报文唯一标识，同一 nonce 第二次登记即为重放
     * @param timestamp 报文时间戳，用于过期回收
     * @param ttlMillis 生存期：早于 {@code timestamp - ttlMillis} 的记录会被回收
     * @return true 表示首次登记成功；false 表示此前已登记（重放）或容量已满
     */
    boolean registerIfAbsent(String nonce, long timestamp, long ttlMillis);
}
