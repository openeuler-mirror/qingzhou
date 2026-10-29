package qingzhou.ai.mcp.client;

import qingzhou.http.client.*;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class McpClient {
    private static final String PREFERRED_VERSION = "2025-06-18";
    private static final String FALLBACK_VERSION = "2025-03-26";
    private static final String UNSUPPORTED_VERSION = "2024-11-05";
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String SSE_DATA = "data:";
    private static final int READ_TIMEOUT = 60_000;
    private static final int AWAIT_TIMEOUT = READ_TIMEOUT + 5_000;
    private static final int ERROR_BODY_CHARS = 200;

    private final McpConfig config;
    private final HttpClient httpClient;
    private final Json json;
    private final Logger logger;

    private final AtomicLong requestIds = new AtomicLong();
    private volatile boolean initialized;
    private volatile String protocolVersion;
    private volatile String sessionId;
    private volatile String serverDescription;

    McpClient(McpConfig config, HttpClient httpClient, Json json, Logger logger) {
        this.config = config;
        this.httpClient = httpClient;
        this.json = json;
        this.logger = logger;
    }

    List<Map<String, Object>> listTools() throws Exception {
        return withSession(() -> {
            List<Map<String, Object>> tools = new ArrayList<>();
            String cursor = null;
            do {
                Map<String, Object> params = null;
                if (cursor != null) {
                    params = new HashMap<>();
                    params.put("cursor", cursor);
                }
                Map<String, Object> result = request("tools/list", params);
                Object page = result.get("tools");
                if (page instanceof List) {
                    for (Object item : (List<?>) page) {
                        if (item instanceof Map) tools.add((Map<String, Object>) item);
                    }
                }
                cursor = text(result.get("nextCursor"));
            } while (cursor != null && !cursor.isEmpty());
            return tools;
        });
    }

    String callTool(String toolName, Map<String, Object> arguments) throws Exception {
        return withSession(() -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", toolName);
            params.put("arguments", arguments == null ? Collections.emptyMap() : arguments);
            return McpToolConverter.extractText(request("tools/call", params));
        });
    }

    private <T> T withSession(Callable<T> action) throws Exception {
        try {
            initialize();
            return action.call();
        } catch (Exception e) {
            if (!isSessionExpired(e)) throw e;

            logger.warn("mcp session of [" + config.id + "] expired, reconnecting");
            initialized = false;
            sessionId = null;
            initialize();
            return action.call();
        }
    }

    private synchronized void initialize() throws Exception {
        if (initialized) return;

        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", PREFERRED_VERSION);
        params.put("capabilities", Collections.emptyMap());
        params.put("clientInfo", clientInfo());


        long id = requestIds.incrementAndGet();
        Collector collector = new Collector(id);
        Response response = post("initialize", params, id, collector);
        try {
            Map<String, Object> result = await(response, collector);
            String negotiated = text(result.get("protocolVersion"));
            if (UNSUPPORTED_VERSION.equals(negotiated)) {
                throw new IOException("mcp server [" + config.id + "] negotiated " + negotiated
                        + ", which requires the deprecated HTTP+SSE transport");
            }
            protocolVersion = negotiated == null ? FALLBACK_VERSION : negotiated;
            sessionId = response.getHeader(SESSION_HEADER);
            serverDescription = descriptionFrom(result);
            initialized = true;
        } finally {
            response.cancel();
        }

        notifyInitialized();
    }

    private void notifyInitialized() {
        try {
            Response response = post("notifications/initialized", null, null, null);
            int status = response.getStatus();
            response.cancel();
            if (status < 200 || status >= 300) {
                logger.warn("mcp server [" + config.id + "] rejected notifications/initialized: status " + status);
            }
        } catch (Exception e) {
            logger.warn("failed to send notifications/initialized to [" + config.id + "]: " + e.getMessage());
        }
    }

    private Map<String, Object> request(String method, Map<String, Object> params) throws Exception {
        long id = requestIds.incrementAndGet();
        Collector collector = new Collector(id);
        Response response = post(method, params, id, collector);
        try {
            return await(response, collector);
        } finally {
            response.cancel();
        }
    }

    private Response post(String method, Map<String, Object> params, Long id, ClientListener listener) throws Exception {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        if (id != null) envelope.put("id", id);
        envelope.put("method", method);
        if (params != null) envelope.put("params", params);

        Request request = httpClient.buildRequest(config.url)
                .method(HttpMethod.POST)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .readTimeout(READ_TIMEOUT)
                .trustAllCertificates()
                .maxBodySize(0);
        config.headers.forEach(request::header);
        if (protocolVersion != null) request.header(PROTOCOL_HEADER, protocolVersion);
        if (sessionId != null) request.header(SESSION_HEADER, sessionId);
        request.body(json.toJson(envelope).getBytes(StandardCharsets.UTF_8));

        return listener == null ? httpClient.send(request) : httpClient.send(request, listener);
    }

    private Map<String, Object> await(Response response, Collector collector) throws Exception {
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            throw new IOException("mcp server [" + config.id + "] returned status " + status + ": " + errorBody(response));
        }

        Map<String, Object> message;
        if (collector != null) {
            if (!collector.awaitResponse()) {
                throw new IOException("timeout waiting for response of [" + config.id + "]");
            }
            if (collector.failure != null) {
                throw new IOException("mcp request to [" + config.id + "] failed: " + collector.failure.getMessage(), collector.failure);
            }
            message = collector.message;
        } else {
            String res = new String(response.getBody(), StandardCharsets.UTF_8);
            message = json.fromJson(res, Map.class);
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

    private String errorBody(Response response) {
        byte[] body = response.getBody();
        if (body == null || body.length == 0) return "";
        String text = new String(body, StandardCharsets.UTF_8);
        return text.length() > ERROR_BODY_CHARS ? text.substring(0, ERROR_BODY_CHARS) : text;
    }

    private Map<String, Object> parseMessage(String line) {
        String data = line.trim();
        if (data.isEmpty()) return null;
        if (data.startsWith(SSE_DATA)) data = data.substring(SSE_DATA.length()).trim(); // SSE 控制行（event/id）无 JSON，自然被跳过
        if (data.isEmpty()) return null;

        try {
            return json.fromJson(data, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSessionExpired(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;

        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("status 404") || lower.contains("session");
    }

    private static Map<String, Object> clientInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "qingzhou");
        info.put("version", System.getProperty("qingzhou.version", "1.0.0"));
        return info;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    String getServerDescription() {
        try {
            initialize();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return serverDescription;
    }

    private static String descriptionFrom(Map<String, Object> result) {
        String instructions = text(result.get("instructions"));
        if (instructions != null && !instructions.trim().isEmpty()) return instructions.trim();

        Object infoNode = result.get("serverInfo");
        if (infoNode instanceof Map) {
            Map<?, ?> info = (Map<?, ?>) infoNode;
            String name = text(info.get("name"));
            String version = text(info.get("version"));
            if (name != null && !name.isEmpty()) {
                return version == null || version.isEmpty() ? name : name + " " + version;
            }
        }
        return null;
    }

    private final class Collector implements ClientListener {
        private final long expectedId;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Map<String, Object> message;
        private volatile Throwable failure;

        Collector(long expectedId) {
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

        boolean awaitResponse() throws InterruptedException {
            return latch.await(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }
}
