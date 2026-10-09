package qingzhou.crypto.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import qingzhou.crypto.NonceRegistry;

/**
 * {@link NonceRegistry} 的默认实现。
 * <p>
 * 只回收已过期的记录：丢弃仍在有效时间窗内的 nonce 等于自己开一个重放窗口；
 * 过期回收后仍超容量时拒绝新登记（容量可按部署调大），而非丢弃有效记录。
 */
final class NonceRegistryImpl implements NonceRegistry {
    /**
     * 容量上限：仅作内存兜底，安全边界是调用方的时间窗
     */
    private static final int MAX_TRACKED_NONCES = 100_000;
    private static final long PRUNE_INTERVAL_MILLIS = 2_000; // 回收的最小间隔（摊销）

    // nonce → 登记时间。CHM 的 putIfAbsent 提供原子登记，热路径无全局锁
    private final Map<String, Long> nonces = new ConcurrentHashMap<>();
    private volatile long lastPruneAt;

    @Override
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
