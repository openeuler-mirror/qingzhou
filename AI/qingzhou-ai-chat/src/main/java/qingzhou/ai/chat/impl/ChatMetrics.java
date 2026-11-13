package qingzhou.ai.chat.impl;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话链路共享计数：写入点分散在监听器与看门狗，采集点在同一 bundle，故收敛到单一类。
 * 会话对象按请求创建，计数须落在 bundle 级单例上才能跨会话累计。
 */
final class ChatMetrics {
    private static final AtomicLong STREAMS_ACTIVE = new AtomicLong();
    private static final AtomicLong HARD_TIMEOUTS = new AtomicLong();
    private static final AtomicLong FIRST_CONTENT_TIMEOUTS = new AtomicLong();
    private static final AtomicLong CLIENT_DISCONNECTS = new AtomicLong();

    private ChatMetrics() {
    }

    static void streamOpened() {
        STREAMS_ACTIVE.incrementAndGet();
    }

    static void streamClosed() {
        STREAMS_ACTIVE.decrementAndGet();
    }

    static void hardTimeout() {
        HARD_TIMEOUTS.incrementAndGet();
    }

    static void firstContentTimeout() {
        FIRST_CONTENT_TIMEOUTS.incrementAndGet();
    }

    static void clientDisconnect() {
        CLIENT_DISCONNECTS.incrementAndGet();
    }

    static long streamsActive() {
        return STREAMS_ACTIVE.get();
    }

    static long hardTimeouts() {
        return HARD_TIMEOUTS.get();
    }

    static long firstContentTimeouts() {
        return FIRST_CONTENT_TIMEOUTS.get();
    }

    static long clientDisconnects() {
        return CLIENT_DISCONNECTS.get();
    }
}
