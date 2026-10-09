package qingzhou.http.server.util;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于来源 key 的失败锁定器：固定窗口内失败次数达到上限即锁定，成功后清零。
 * <p>
 * 固定窗口：窗口过期后在下次判定/记录时重置计数，重新获得完整失败预算。
 * 容量有界：key 数超过上限时，先回收窗口已过期的记录（零代价），
 * 仍超员再按「最久未失败」优先淘汰到低水位（上限的 90%），摊销全表遍历。
 * <p>
 * 线程安全：内部状态为不可变快照，经 {@code ConcurrentHashMap.compute} 原子替换，
 * 读取无锁且不存在 long 撕裂风险。
 */
public final class FailureLockout {
    private final int maxFailures;
    private final long windowMillis;
    private final int maxTrackedKeys;
    private final int evictWatermark; // 淘汰低水位：上限的 90%
    private final Map<String, LockState> states = new ConcurrentHashMap<>();

    /**
     * @param maxFailures    窗口内允许的失败次数，达到即锁定，须 >= 1
     * @param windowMillis   滑动窗口长度（毫秒），须 >= 1
     * @param maxTrackedKeys 跟踪的 key 数上限，达到即触发淘汰，须 >= 1
     */
    public FailureLockout(int maxFailures, long windowMillis, int maxTrackedKeys) {
        if (maxFailures < 1 || windowMillis < 1 || maxTrackedKeys < 1) {
            throw new IllegalArgumentException("lockout limits must be positive");
        }
        this.maxFailures = maxFailures;
        this.windowMillis = windowMillis;
        this.maxTrackedKeys = maxTrackedKeys;
        this.evictWatermark = Math.max(1, maxTrackedKeys * 9 / 10);
    }

    /**
     * 该 key 是否处于锁定中（窗口内失败次数已达上限）。
     */
    public boolean isLocked(String key) {
        LockState state = states.get(key);
        return state != null && !state.expired(now()) && state.count >= maxFailures;
    }

    /**
     * 记录一次失败。
     */
    public void recordFailure(String key) {
        states.compute(key, (k, state) -> {
            long now = now();
            LockState next;
            if (state == null || state.expired(now)) {
                next = new LockState(1, now, now, windowMillis); // 窗口过期等同无记录：重置计数、重新起算窗口
            } else {
                next = new LockState(state.count + 1, state.windowStart, now, windowMillis);
            }
            return next;
        });
        evictOverflow();
    }

    /**
     * 清除该 key 的计数（认证/登录成功后调用）。
     */
    public void clear(String key) {
        states.remove(key);
    }

    // 超限后先回收窗口过期项（限流语义上已自动失效，删除零代价），
    // 仍超员再按最久未失败优先淘汰到低水位：无兜底时大量不同来源的持续失败洪水可令 map 无界增长
    private void evictOverflow() {
        if (states.size() <= maxTrackedKeys) return;
        long expireBefore = now() - windowMillis;
        states.entrySet().removeIf(e -> e.getValue().lastFailureAt < expireBefore);

        int excess = states.size() - evictWatermark;
        if (excess <= 0) return;
        // 有界堆一次遍历选出最久未失败的 excess 个：堆序取倒序（堆顶为最近失败），溢出时弹出的是较新者
        PriorityQueue<Map.Entry<String, LockState>> oldest = new PriorityQueue<>(excess + 1,
                Comparator.<Map.Entry<String, LockState>>comparingLong(e -> e.getValue().lastFailureAt).reversed());
        for (Map.Entry<String, LockState> entry : states.entrySet()) {
            oldest.offer(entry);
            if (oldest.size() > excess) oldest.poll();
        }
        for (Map.Entry<String, LockState> entry : oldest) states.remove(entry.getKey());
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    // 不可变快照：compute 内整体替换，读取无锁且无 long 撕裂风险（JLS 17.7）
    private static final class LockState {
        private final int count;
        private final long windowStart;
        private final long lastFailureAt;
        private final long windowMillis;

        private LockState(int count, long windowStart, long lastFailureAt, long windowMillis) {
            this.count = count;
            this.windowStart = windowStart;
            this.lastFailureAt = lastFailureAt;
            this.windowMillis = windowMillis;
        }

        private boolean expired(long now) {
            return now - windowStart > windowMillis;
        }
    }
}
