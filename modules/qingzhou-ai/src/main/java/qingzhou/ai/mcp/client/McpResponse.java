package qingzhou.ai.mcp.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import qingzhou.http.client.ClientListener;
import qingzhou.json.Json;

/**
 * 收集单个请求的响应：onBody/onComplete/onError 只由流式读取线程触发，
 * 字段可见性由 CountDownLatch 的 happens-before 保证，故不加 volatile。
 */
public class McpResponse implements ClientListener {
    private static final String SSE_DATA = "data:";
    private static final int AWAIT_TIMEOUT = McpClient.READ_TIMEOUT + 5_000;

    private final Json json;
    private final McpConfig config;
    private final long expectedId;

    private final CountDownLatch latch = new CountDownLatch(1);
    private Map<String, Object> message;
    private Throwable failure;

    McpResponse(Json json, McpConfig config, long expectedId) {
        this.json = json;
        this.config = config;
        this.expectedId = expectedId;
    }

    @Override
    public void onBody(String line) {
        Map<String, Object> parsed = parseMessage(line);
        if (parsed == null) return;

        Object id = parsed.get("id");
        if (id instanceof Number && ((Number) id).longValue() == expectedId) {
            message = parsed;
            latch.countDown();
        }
    }

    @Override
    public void onComplete() {
        latch.countDown();
    }

    @Override
    public void onError(Throwable t) {
        failure = t;
        latch.countDown();
    }

    private Map<String, Object> parseMessage(String line) {
        String data = line.trim();
        if (data.startsWith(SSE_DATA)) data = data.substring(SSE_DATA.length()).trim();
        if (data.isEmpty()) return null;

        try {
            return json.fromJson(data, Map.class);
        } catch (Exception e) {
            return null; // SSE 控制行（event/id）无 JSON
        }
    }

    Map<String, Object> await() throws Exception {
        if (!latch.await(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new IOException("timeout waiting for response of [" + config.id + "]");
        }
        if (failure != null) {
            throw new IOException("mcp request to [" + config.id + "] failed: " + failure.getMessage(), failure);
        }
        if (message == null) {
            throw new IOException("mcp server [" + config.id + "] sent no response");
        }

        Object error = message.get("error");
        if (error instanceof Map) {
            Map<?, ?> errorMap = (Map<?, ?>) error;
            throw new IOException("mcp server [" + config.id + "] error [" + errorMap.get("code") + "]: " + errorMap.get("message"));
        }
        Object result = message.get("result");
        if (!(result instanceof Map)) {
            throw new IOException("mcp server [" + config.id + "] sent invalid result");
        }
        return (Map<String, Object>) result;
    }
}
