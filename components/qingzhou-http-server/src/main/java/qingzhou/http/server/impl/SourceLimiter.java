package qingzhou.http.server.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 逐来源（remote host）在途请求并发限制：全局并发额度无来源维度，单 IP 的慢速/并发请求即可耗尽配额造成拒绝服务。
 * 容量有界：超员时淘汰空闲项；在途项永不淘汰——淘汰在途项会使其重获完整配额，反而削弱限制。
 * 线程安全：计数经 ConcurrentHashMap 的 CAS 替换与 computeIfPresent 原子更新。
 */
final class SourceLimiter {
    private final int maxConcurrent; // 0 表示不限制
    private final int maxSources;
    private final Map<String, Integer> sources = new ConcurrentHashMap<>();

    SourceLimiter(int maxConcurrent, int maxSources) {
        this.maxConcurrent = maxConcurrent;
        this.maxSources = maxSources;
    }

    boolean isEnabled() {
        return maxConcurrent > 0;
    }

    // 返回 false 表示该来源在途请求已达上限
    boolean tryAcquire(String key) {
        while (true) {
            Integer cur = sources.get(key);
            if (cur != null && cur >= maxConcurrent) return false;
            Integer next = cur == null ? 1 : cur + 1;
            if (cur == null) {
                if (sources.putIfAbsent(key, next) == null) {
                    evictOverflow();
                    return true;
                }
            } else if (sources.replace(key, cur, next)) {
                evictOverflow();
                return true;
            }
        }
    }

    void release(String key) {
        sources.computeIfPresent(key, (k, cur) -> cur - 1);
    }

    // 超员时淘汰空闲项（计数为 0）；条件删除避免误删快照后被重新占用的在途项
    private void evictOverflow() {
        if (sources.size() <= maxSources) return;
        for (Map.Entry<String, Integer> entry : sources.entrySet()) {
            if (entry.getValue() == 0
                    && sources.remove(entry.getKey(), 0)
                    && sources.size() <= maxSources) return;
        }
    }
}
