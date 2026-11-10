package qingzhou.ai.skill.mcp.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import qingzhou.http.client.*;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

final class McpClient {
    static final int READ_TIMEOUT = 60_000;
    private static final String FALLBACK_VERSION = "2025-03-26";
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final int ERROR_BODY_CHARS = 200;
    private static final int MAX_RESULT_CHARS = 20_000;

    private final McpConfig config;
    private final HttpClient httpClient;
    private final Json json;
    private final Logger logger;
    private final String qzVersion;

    private final AtomicLong requestIds = new AtomicLong();
    private volatile String protocolVersion; // 非 null 即已完成 initialize
    private volatile String sessionId;

    McpClient(McpConfig config, HttpClient httpClient, Json json, Logger logger, String qzVersion) {
        this.config = config;
        this.httpClient = httpClient;
        this.json = json;
        this.logger = logger;
        this.qzVersion = qzVersion;
    }

    List<Map<String, Object>> listTools() throws Exception {
        return withSession(() -> {
            List<Map<String, Object>> tools = new ArrayList<>();
            String cursor = null;
            do {
                Map<String, Object> params = cursor == null ? null : Collections.singletonMap("cursor", cursor);
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
            return extractContent(request("tools/call", params));
        });
    }

    private <T> T withSession(Callable<T> action) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                initialize();
                return action.call();
            } catch (Exception e) {
                if (attempt > 0 || !(e instanceof SessionLostException)) throw e;

                logger.warn("mcp session of [" + config.id + "] expired, reconnecting");
                protocolVersion = null; // 强制重连后重新 initialize，服务端已丢失会话
                sessionId = null;
            }
        }
    }

    private synchronized void initialize() throws Exception {
        if (protocolVersion != null) return;

        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("name", "qingzhou");
        clientInfo.put("version", qzVersion);
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2025-06-18");
        params.put("capabilities", Collections.emptyMap());
        params.put("clientInfo", clientInfo);

        Map<String, Object> result = request("initialize", params);
        String negotiated = text(result.get("protocolVersion"));
        if (negotiated != null && negotiated.compareTo(FALLBACK_VERSION) < 0) { // 2025-03-26 之前为废弃的 HTTP+SSE 传输
            throw new IOException("mcp server [" + config.id + "] negotiated " + negotiated
                    + ", which predates the Streamable HTTP transport");
        }
        protocolVersion = negotiated == null ? FALLBACK_VERSION : negotiated;
        notifyInitialized();
    }

    private void notifyInitialized() {
        try {
            int status = send("notifications/initialized", null, null, null).getStatus();
            if (status < 200 || status >= 300) {
                logger.warn("mcp server [" + config.id + "] rejected notifications/initialized: status " + status);
            }
        } catch (Exception e) {
            logger.warn("failed to send notifications/initialized to [" + config.id + "]: " + e.getMessage());
        }
    }

    private static String extractContent(Map<String, Object> result) throws IOException {
        Object content = result.get("content");
        StringBuilder text = new StringBuilder();
        for (Object item : content instanceof List ? (List<?>) content : Collections.emptyList()) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> part = (Map<?, ?>) item;
            if (text.length() > 0) text.append('\n');
            text.append("text".equals(part.get("type"))
                    ? Objects.toString(part.get("text"), "")
                    : "[" + part.get("type") + " content]");
        }

        String value = text.length() > MAX_RESULT_CHARS
                ? text.substring(0, MAX_RESULT_CHARS) + "\n…（内容过长已截断）" : text.toString();
        if (Boolean.TRUE.equals(result.get("isError")))
            throw new IOException(value.isEmpty() ? "mcp tool reported an error" : value);
        return value;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> request(String method, Map<String, Object> params) throws Exception {
        long id = requestIds.incrementAndGet();
        McpResponse mcpResponse = new McpResponse(json, config, id);
        Response response = send(method, params, id, mcpResponse);
        try {
            int status = response.getStatus();
            if (status < 200 || status >= 300) { // 非 2xx 不进入流式读取，无可取消的响应
                String body = new String(response.getBody(), StandardCharsets.UTF_8);
                String message = "mcp server [" + config.id + "] returned status " + status + ": "
                        + body.substring(0, Math.min(body.length(), ERROR_BODY_CHARS));
                throw status == 404 ? new SessionLostException(message) : new IOException(message); // 404 是 MCP 约定的会话失效
            }

            return mcpResponse.await();
        } finally {
            response.cancel();
        }
    }

    private Response send(String method, Map<String, Object> params, Long id, ClientListener listener) throws Exception {
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
                .trustAllCertificates() // 受控内网降级：需 allow_ssl_trust_all=true
                .maxBodySize(MAX_RESPONSE_BYTES);
        config.headers.forEach(request::header);
        if (protocolVersion != null) request.header(PROTOCOL_HEADER, protocolVersion);
        if (sessionId != null) request.header(SESSION_HEADER, sessionId);
        request.body(json.toJson(envelope).getBytes(StandardCharsets.UTF_8));

        Response response = httpClient.send(request, listener); // listener 为 null 即同步整读
        String session = response.getHeader(SESSION_HEADER); // 会话 id 仅由服务端在 initialize 响应下发
        if (session != null) sessionId = session;
        return response;
    }

    private static final class SessionLostException extends IOException {
        SessionLostException(String message) {
            super(message);
        }
    }
}
