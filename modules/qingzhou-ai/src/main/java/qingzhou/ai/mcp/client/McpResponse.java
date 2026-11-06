package qingzhou.ai.mcp.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import qingzhou.http.client.ClientListener;
import qingzhou.json.Json;

/**
 * 收集单个请求的响应：回调只由流式读取线程触发，countDown 之后不再写字段，
 * 可见性由 CountDownLatch 的 happens-before 保证，故不加 volatile。
 */
public class McpResponse implements ClientListener {
    private static final String SSE_DATA = "data:";
    private static final int AWAIT_TIMEOUT = McpClient.READ_TIMEOUT + 5_000;

    private final Json json;
    private final McpConfig config;
    private final long expectedId;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final StringBuilder pending = new StringBuilder();
    private Map<String, Object> message;
    private Throwable failure;

    McpResponse(Json json, McpConfig config, long expectedId) {
        this.json = json;
        this.config = config;
        this.expectedId = expectedId;
    }

    @Override
    public void onBody(String line) {
        if (message != null || failure != null) return; // 首条响应既定，后续流式内容不再参与

        String data = line.trim();
        if (data.isEmpty() || data.startsWith("event:")) { // SSE 事件边界：丢弃未收齐的残片
            pending.setLength(0);
            return;
        }
        if (data.startsWith(SSE_DATA)) {
            data = data.substring(SSE_DATA.length()).trim();
            if (data.isEmpty()) return;
        } else if (!data.startsWith("{")) {
            return; // id:/retry:/注释等非数据行
        }
        if (pending.length() > 0) pending.append('\n');
        pending.append(data);

        Map<String, Object> parsed;
        try {
            parsed = json.fromJson(pending.toString(), Map.class);
        } catch (Exception e) {
            return; // 跨行 JSON 尚未收齐，由后续行或 onComplete 收口
        }
        pending.setLength(0);
        if (!parsed.containsKey("result") && !parsed.containsKey("error")) return; // 通知/服务端请求
        if (!Long.toString(expectedId).equals(String.valueOf(parsed.get("id")))) return; // 缺失/异号一律拒绝

        message = parsed;
        latch.countDown();
    }

    @Override
    public void onComplete() {
        if (message == null && failure == null && pending.length() > 0) {
            failure = new IOException("mcp server [" + config.id + "] sent invalid response");
        }
        latch.countDown();
    }

    @Override
    public void onError(Throwable t) {
        if (message != null || failure != null) return;
        failure = t;
        latch.countDown();
    }

    Map<String, Object> await() throws Exception {
        if (!latch.await(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
            throw new IOException("timeout waiting for response of [" + config.id + "]");
        }
        if (failure != null) {
            throw new IOException("mcp request to [" + config.id + "] failed: " + failure.getMessage(), failure);
        }
        Map<String, Object> response = message;
        if (response == null) {
            throw new IOException("mcp server [" + config.id + "] sent no response");
        }

        Object error = response.get("error");
        if (error instanceof Map) {
            Map<?, ?> errorMap = (Map<?, ?>) error;
            throw new IOException("mcp server [" + config.id + "] error [" + errorMap.get("code") + "]: " + errorMap.get("message"));
        }
        Object result = response.get("result");
        if (!(result instanceof Map)) {
            throw new IOException("mcp server [" + config.id + "] sent invalid result");
        }
        return (Map<String, Object>) result;
    }
}
