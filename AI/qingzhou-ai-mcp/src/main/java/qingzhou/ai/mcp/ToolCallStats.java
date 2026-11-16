package qingzhou.ai.mcp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具调用口径的唯一写入口：失败按工具名分列，取值域受注册表约束，基数受控。
 */
class ToolCallStats {
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong forbidden = new AtomicLong();
    private final Map<String, AtomicLong> failures = new ConcurrentHashMap<>();

    void onCall() {
        calls.incrementAndGet();
    }

    void onForbidden() {
        forbidden.incrementAndGet();
    }

    void onFailure(String toolName) {
        failures.computeIfAbsent(toolName, key -> new AtomicLong()).incrementAndGet();
    }

    long calls() {
        return calls.get();
    }

    long forbidden() {
        return forbidden.get();
    }

    Map<String, Long> failures() {
        Map<String, Long> snapshot = new HashMap<>();
        failures.forEach((toolName, count) -> snapshot.put(toolName, count.get()));
        return snapshot;
    }
}
