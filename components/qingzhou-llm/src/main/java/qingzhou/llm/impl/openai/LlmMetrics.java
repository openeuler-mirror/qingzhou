package qingzhou.llm.impl.openai;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM 调用口径的唯一写入口：ChatModel 按请求创建，计数只能落在 bundle 级单例上；
 * 压缩期摘要走同步调用，故统计不能挂在会话监听器上，否则 token 与调用量会系统性低估。
 */
final class LlmMetrics {
    private static final AtomicLong REQUESTS = new AtomicLong();
    private static final AtomicLong RETRIES = new AtomicLong();
    private static final AtomicLong STREAM_INTERRUPTIONS = new AtomicLong();
    private static final AtomicLong PROMPT_TOKENS = new AtomicLong();
    private static final AtomicLong COMPLETION_TOKENS = new AtomicLong();
    // 失败原因取值域为 HTTP 状态码加固定字面量，基数受控
    private static final Map<String, AtomicLong> FAILURES = new ConcurrentHashMap<>();

    private LlmMetrics() {
    }

    static void request() {
        REQUESTS.incrementAndGet();
    }

    static void retry() {
        RETRIES.incrementAndGet();
    }

    static void streamInterruption() {
        STREAM_INTERRUPTIONS.incrementAndGet();
    }

    static void failure(String reason) {
        FAILURES.computeIfAbsent(reason, key -> new AtomicLong()).incrementAndGet();
    }

    // 仅流式响应携带 usage；同步调用不解析用量，故不计入
    static void usage(int promptTokens, int completionTokens) {
        PROMPT_TOKENS.addAndGet(promptTokens);
        COMPLETION_TOKENS.addAndGet(completionTokens);
    }

    static long requests() {
        return REQUESTS.get();
    }

    static long retries() {
        return RETRIES.get();
    }

    static long streamInterruptions() {
        return STREAM_INTERRUPTIONS.get();
    }

    static long promptTokens() {
        return PROMPT_TOKENS.get();
    }

    static long completionTokens() {
        return COMPLETION_TOKENS.get();
    }

    static Map<String, Long> failures() {
        Map<String, Long> snapshot = new HashMap<>();
        FAILURES.forEach((reason, count) -> snapshot.put(reason, count.get()));
        return snapshot;
    }
}
