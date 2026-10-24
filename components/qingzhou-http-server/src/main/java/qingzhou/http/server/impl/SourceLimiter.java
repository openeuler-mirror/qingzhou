package qingzhou.http.server.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 逐来源（remote host）在途请求并发限制：全局并发额度无来源维度，单 IP 的慢速/并发请求即可耗尽配额造成拒绝服务。
 * <p>
 * 容量有界：超员时先回收过期空闲项，仍超员按最久未活动淘汰空闲项；在途项永不淘汰——淘汰在途项会使其重获完整配额，反而削弱限制。
 * 线程安全：内部状态为不可变快照，经 ConcurrentHashMap 原子替换。
 */
final class SourceLimiter {
    private final int maxConcurrent; // 0 表示不限制
    private final int maxSources;
    private final long idleMillis;

    private final Map<String, Source> sources = new ConcurrentHashMap<>();

    SourceLimiter(int maxConcurrent, int maxSources, long idleSeconds) {
        this.maxConcurrent = maxConcurrent;
        this.maxSources = maxSources;
        this.idleMillis = idleSeconds * 1000L;
    }

    boolean isEnabled() {
        return maxConcurrent > 0;
    }

    // 返回 false 表示该来源在途请求已达上限
    boolean tryAcquire(String key) {
        while (true) {
            Source current = sources.get(key);
            if (current != null && current.inFlight >= maxConcurrent) return false;
            Source next = new Source(current == null ? 1 : current.inFlight + 1);
            if (current == null) {
                if (sources.putIfAbsent(key, next) != null) continue; // 并发写入：基于最新快照重试
            } else if (!sources.replace(key, current, next)) {
                continue;
            }
            evictOverflow();
            return true;
        }
    }

    void release(String key) {
        while (true) {
            Source current = sources.get(key);
            if (current == null) return; // 已被淘汰：计数随之丢弃，不影响正确性
            if (sources.replace(key, current, new Source(current.inFlight - 1))) return;
        }
    }

    // 仅空闲项可淘汰：快照排序避免排序期间键被并发移除导致空读；条件删除避免误删快照后被重新占用的在途项
    private void evictOverflow() {
        if (sources.size() <= maxSources) return;
        long expireBefore = System.currentTimeMillis() - idleMillis;
        List<Map.Entry<String, Source>> idle = new ArrayList<>();
        for (Map.Entry<String, Source> entry : sources.entrySet()) {
            if (entry.getValue().inFlight == 0) idle.add(entry);
        }
        idle.sort(Comparator.comparingLong(entry -> entry.getValue().lastActiveAt));
        int excess = sources.size() - maxSources;
        for (Map.Entry<String, Source> entry : idle) {
            if (entry.getValue().lastActiveAt < expireBefore || excess > 0) {
                if (sources.remove(entry.getKey(), entry.getValue())) excess--;
            }
        }
    }

    // 不可变快照：replace 原子换代，读取无锁且不存在计数撕裂
    private static final class Source {
        private final int inFlight;
        private final long lastActiveAt;

        Source(int inFlight) {
            this.inFlight = inFlight;
            this.lastActiveAt = System.currentTimeMillis();
        }
    }
}
