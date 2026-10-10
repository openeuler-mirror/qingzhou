package qingzhou.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防重放的去重登记器：按 nonce 原子登记，与具体加密方式无关。
 * <p>
 * 供防重放报文封装 ReplayGuardCipher（AES-GCM）与注册协议（RSA 引导报文）共用：
 * 两者时间窗不同，故 TTL 由调用方按自身窗口给出（通常取窗口的两倍）。
 * <p>
 * 只回收已过期的记录：丢弃仍在有效时间窗内的 nonce 等于自己开一个重放窗口；
 * 过期回收后仍超容量时拒绝新登记（容量可按部署调大），而非丢弃有效记录。
 */
public class NonceRegistry {
    /**
     * 容量上限：仅作内存兜底，安全边界是调用方的时间窗
     */
    private static final int MAX_TRACKED_NONCES = 100_000;
    private static final long PRUNE_INTERVAL_MILLIS = 2_000; // 回收的最小间隔（摊销）

    // nonce → 登记时间。CHM 的 putIfAbsent 提供原子登记，热路径无全局锁
    private final Map<String, Long> nonces = new ConcurrentHashMap<>();
    private volatile long lastPruneAt;

    /**
     * 原子登记 nonce。
     *
     * @param nonce     报文唯一标识，同一 nonce 第二次登记即为重放
     * @param timestamp 报文时间戳，用于过期回收
     * @param ttlMillis 生存期：早于 {@code timestamp - ttlMillis} 的记录会被回收
     * @return true 表示首次登记成功；false 表示此前已登记（重放）或容量已满
     */
    public boolean registerIfAbsent(String nonce, long timestamp, long ttlMillis) {
        pruneExpired(timestamp, ttlMillis);
        if (nonces.size() > MAX_TRACKED_NONCES) return false;
        return nonces.putIfAbsent(nonce, timestamp) == null;
    }

    private void pruneExpired(long now, long ttlMillis) {
        if (nonces.size() <= MAX_TRACKED_NONCES || now - lastPruneAt < PRUNE_INTERVAL_MILLIS) return;
        lastPruneAt = now;
        long expireBefore = now - ttlMillis;
        nonces.values().removeIf(ts -> ts < expireBefore);
    }
}
