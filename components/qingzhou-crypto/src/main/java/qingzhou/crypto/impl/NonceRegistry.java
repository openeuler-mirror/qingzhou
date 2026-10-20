package qingzhou.crypto.impl;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 nonce 原子去重的登记器，与具体加密方式和时间基准无关。
 * <p>
 * TTL 由调用方按自身时间窗给出（通常取窗口两倍），自登记时刻起按单调时钟计算，
 * 不受系统时间调整影响。
 * <p>
 * 只回收已过期的记录：丢弃仍在有效时间窗内的 nonce 等于自己开一个重放窗口；
 * 容量满时先清理过期项，仍超容量再按到期时间淘汰最早到期的记录（而非拒绝新登记），
 * 极端洪水下被淘汰项的重放窗口以可用性为优先，容量可按部署调大。
 */
class NonceRegistry {
    /**
     * 容量上限：仅作内存兜底，安全边界是调用方的时间窗
     */
    private static final int MAX_TRACKED_NONCES = 100_000;

    // nonce → 到期时刻（单调时钟毫秒）。CHM 的 putIfAbsent 提供原子登记，热路径无全局锁。
    private final Map<String, Long> nonces = new ConcurrentHashMap<>();

    /**
     * 原子登记 nonce。
     *
     * @param nonce     报文唯一标识，同一 nonce 第二次登记即为重放
     * @param ttlMillis 生存期：自登记时刻起保留多久，须覆盖该 nonce 对应报文仍可能被受理的最长时间
     * @return true 表示首次登记成功；false 表示此前已登记（重放）
     */
    boolean registerIfAbsent(String nonce, long ttlMillis) {
        // 只在容量满时进锁：热路径仍无锁；双检避免多线程同时淘汰，把表削到远低于上限
        if (nonces.size() >= MAX_TRACKED_NONCES) {
            synchronized (this) {
                pruneExpired(); // 先清过期项，避免直接淘汰仍在有效期内的 nonce
                if (nonces.size() >= MAX_TRACKED_NONCES) evictEarliestExpiring();
            }
        }
        return nonces.putIfAbsent(nonce, nowMillis() + ttlMillis) == null;
    }

    // 单调时钟：TTL 与回收不受 NTP 等系统时间调整影响
    private long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    private void pruneExpired() {
        long now = nowMillis();
        nonces.values().removeIf(expireAt -> expireAt < now);
    }

    // 有界堆一次遍历选出最早到期的 n 条（O(n log k)）：全量拷贝排序（O(n log n)）会在解密热路径上造成停顿
    private void evictEarliestExpiring() {
        int remove = MAX_TRACKED_NONCES / 10; // 仅在容量满时调用
        PriorityQueue<Map.Entry<String, Long>> candidates = new PriorityQueue<>(remove + 1,
                Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed());
        for (Map.Entry<String, Long> entry : nonces.entrySet()) {
            candidates.offer(entry);
            if (candidates.size() > remove) candidates.poll();
        }
        for (Map.Entry<String, Long> entry : candidates) {
            nonces.remove(entry.getKey(), entry.getValue());
        }
    }
}
