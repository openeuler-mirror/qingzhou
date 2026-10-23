package qingzhou.logger.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * 日志窗口限流：同一「级别 + 消息指纹」在窗口内只放行一次，其余仅计数。
 * 指纹只归一化长数字（时间戳、长 id），状态码与端口等短数字保留，避免语义不同的日志被合并。
 */
final class LogThrottle {
    private static final long FAILURE_WINDOW_MS = 30_000L; // 同一失败消息的聚合窗口
    private static final int MAX_KEYS = 2048; // 消息发散时重置，防 map 无界
    private static final int FINGERPRINT_CHARS = 160;
    private static final Pattern LONG_DIGITS = Pattern.compile("\\d{6,}");

    private final LongSupplier clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    LogThrottle() {
        this(System::currentTimeMillis);
    }

    LogThrottle(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * @return -1 表示本次抑制；>=0 表示放行，值为上一窗口被抑制的条数
     */
    long acquire(char level, String msg) {
        return acquire(level, msg, FAILURE_WINDOW_MS, clock.getAsLong());
    }

    private long acquire(char level, String msg, long windowMs, long now) {
        if (msg == null) return 0;
        String key = level + fingerprint(msg);
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= MAX_KEYS) windows.clear(); // 极端发散时重置，只损失短期聚合精度
            Window created = new Window(now);
            Window existing = windows.putIfAbsent(key, created);
            window = existing != null ? existing : created;
        }
        return window.acquire(now, windowMs);
    }

    private static String fingerprint(String msg) {
        return LONG_DIGITS.matcher(msg.substring(0, Math.min(msg.length(), FINGERPRINT_CHARS))).replaceAll("#");
    }

    private static final class Window {
        private long start;
        private boolean first = true; // 窗口内首条：放行且不计入抑制数
        private long suppressed;

        Window(long start) {
            this.start = start;
        }

        private synchronized long acquire(long now, long windowMs) {
            if (now - start >= windowMs) { // 新窗口：放行并带出上一窗口的抑制条数
                long count = suppressed;
                start = now;
                first = true;
                suppressed = 0;
                return count;
            }
            if (first) {
                first = false;
                return 0;
            }
            suppressed++;
            return -1;
        }
    }
}
