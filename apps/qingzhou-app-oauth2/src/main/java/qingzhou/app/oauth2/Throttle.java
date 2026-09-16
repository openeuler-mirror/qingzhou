package qingzhou.app.oauth2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流：同一来源连续失败超过阈值后锁定，防止口令爆破。
 */
final class Throttle {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 300_000;

    private final Map<String, long[]> failures = new ConcurrentHashMap<>(); // 来源 -> {失败次数, 首次失败时间}

    boolean isLocked(String key) {
        long[] record = failures.get(key);
        if (record == null || record[0] < MAX_FAILURES) return false;

        if (System.currentTimeMillis() - record[1] <= LOCK_MILLIS) return true;

        failures.remove(key); // 锁定期已过，重新计数
        return false;
    }

    void recordFailure(String key) {
        failures.compute(key, (k, record) -> {
            long now = System.currentTimeMillis();
            return record == null || now - record[1] > LOCK_MILLIS
                    ? new long[]{1, now} : new long[]{record[0] + 1, record[1]};
        });
        if (failures.size() > 10_000) { // 惰性清理过期记录，防止不同来源洪水导致内存膨胀
            long now = System.currentTimeMillis();
            failures.entrySet().removeIf(entry -> now - entry.getValue()[1] > LOCK_MILLIS);
        }
    }

    void clear(String key) {
        failures.remove(key);
    }
}
