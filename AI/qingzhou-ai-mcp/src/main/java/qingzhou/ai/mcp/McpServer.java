package qingzhou.ai.mcp;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.Constants;
import qingzhou.ai.ToolCallInterceptionService;
import qingzhou.ai.ToolParameterParser;
import qingzhou.ai.ToolService;
import qingzhou.common.Configs;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

@Component(configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {HttpHandler.HANDLE_PATH + "=/mcp", HttpHandler.HANDLE_METHODS + "=POST"})
public class McpServer implements HttpHandler {
    private static final String JSONRPC_VERSION = "2.0";
    private static final String PROTOCOL_VERSION = "2025-03-26";

    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private ToolCallInterceptionService interceptionService;
    @Reference
    private ToolParameterParser parameterParser;

    private final Map<String, ToolCache> toolCacheMap = new ConcurrentHashMap<>();
    private volatile List<Map<String, Object>> toolSchemas = Collections.emptyList();
    private volatile Map<String, Object> initializeData = Collections.emptyMap();
    private ThreadPoolExecutor workers;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        int workerThreads = (int) configs.between("mcp_worker_threads", 1, 64, 16);
        int capacity = (int) configs.between("mcp_worker_queue", 1, 10_000, 100);
        AtomicInteger seq = new AtomicInteger();
        workers = new ThreadPoolExecutor(workerThreads, workerThreads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), runnable -> {
            Thread thread = new Thread(runnable, "qz-mcp-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });

        String version = config.get("qingzhou.version");
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "qingzhou-mcp-server");
        serverInfo.put("version", version == null || version.isEmpty() ? "unknown" : version);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Collections.singletonMap("listChanged", false));

        Map<String, Object> data = new HashMap<>();
        data.put("protocolVersion", PROTOCOL_VERSION);
        data.put("serverInfo", serverInfo);
        data.put("capabilities", capabilities);
        initializeData = Collections.unmodifiableMap(data);
    }

    @Deactivate
    public void destroy() {
        workers.shutdown(); // 排空而非中断：丢弃排队请求会使其 finish() 永不执行，响应挂到空闲超时
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + ToolService.TOOL_LABEL + "=" + Constants.TOOL_LABEL_INVOKE_ACTION + ")", // 按服务属性过滤
            unbind = "unbindAiTool")
    public synchronized void bindAiTool(ToolService tool, Map<String, Object> properties) {
        ToolCache toolCache = new ToolCache(tool, properties, parameterParser);
        toolCacheMap.put(toolCache.toolName, toolCache);
        rebuildToolSchemas();
    }

    // 显式声明 unbind：动态多引用按服务实例增删
    public synchronized void unbindAiTool(ToolService tool) {
        toolCacheMap.entrySet().removeIf(entry -> entry.getValue().toolService == tool);
        rebuildToolSchemas();
    }

    // 与 bind/unbind 同锁：快照与 map 变更不得交错覆盖
    private void rebuildToolSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>(toolCacheMap.size());
        for (ToolCache toolCache : toolCacheMap.values()) {
            schemas.add(toolCache.toolSchema);
        }
        toolSchemas = Collections.unmodifiableList(schemas);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        byte[] requestBody = httpRequest.getBody();
        httpResponse.async(); // 工具执行可能长时间阻塞，响应由业务线程写并收尾
        try {
            workers.execute(() -> respond(httpRequest, httpResponse, requestBody));
        } catch (RejectedExecutionException e) {
            logger.error("mcp worker pool is exhausted, request rejected", e);
            httpResponse.status(503);
            try {
                send(httpResponse, errorResponse(null, -32603, "Server busy"));
            } catch (Exception writeError) {
                logger.error("failed to write mcp busy response", writeError);
            } finally {
                httpResponse.finish();
            }
        }
    }

    // 业务线程执行：解析、分发与写响应；异步响应必须在此收尾
    private void respond(HttpRequest httpRequest, HttpResponse httpResponse, byte[] requestBody) {
        try {
            process(httpRequest, httpResponse, requestBody);
        } catch (Exception e) {
            logger.error("mcp request failed", e);
            try {
                send(httpResponse, errorResponse(null, -32603, "Internal error"));
            } catch (Exception writeError) {
                logger.error("failed to write mcp error response", writeError);
            }
        } finally {
            httpResponse.finish();
        }
    }

    private void process(HttpRequest httpRequest, HttpResponse httpResponse, byte[] requestBody) throws Exception {
        String body = requestBody == null ? "" : new String(requestBody, StandardCharsets.UTF_8);
        Object parsed;
        try {
            parsed = json.fromJson(body, Object.class);
        } catch (Exception e) {
            httpResponse.status(400); // 非 JSON 无法构成 JSON-RPC 报文，只能以 HTTP 400 携带 -32700
            send(httpResponse, errorResponse(null, -32700, "Parse error"));
            return;
        }

        boolean isBatch = parsed instanceof List;
        List<?> nodes = isBatch ? (List<?>) parsed : Collections.singletonList(parsed);
        if (isBatch && nodes.isEmpty()) {
            httpResponse.status(400); // 空 batch 不构成合法 JSON-RPC 输入
            send(httpResponse, errorResponse(null, -32600, "Invalid Request"));
            return;
        }

        List<Map<String, Object>> replies = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> reply = processNode(node, httpRequest, isBatch);
            if (reply != null) replies.add(reply); // notification/response 不产生响应
        }

        if (replies.isEmpty()) {
            httpResponse.status(202); // 全为 notification/response：按 MCP 传输规范回 202 且无响应体
            return;
        }
        send(httpResponse, isBatch ? replies : replies.get(0));
    }

    // 单条报文处理；返回 null 表示 notification 或 JSON-RPC response
    private Map<String, Object> processNode(Object node, HttpRequest httpRequest, boolean isBatch) {
        if (node instanceof Map) {
            Map<String, Object> message = (Map<String, Object>) node;
            if (!message.containsKey("method")
                    && (message.containsKey("result") || message.containsKey("error"))) {
                return null; // MCP 允许客户端回传 JSON-RPC response：接受但不回复
            }
        }

        Map<String, Object> invalid = invalidRequest(node);
        if (invalid != null) return invalid;

        Map<String, Object> requestMap = (Map<String, Object>) node;
        if (!requestMap.containsKey("id")) return null; // 如握手必须的 notifications/initialized

        Object id = requestMap.get("id");
        String method = (String) requestMap.get("method");
        if (isBatch && "initialize".equals(method)) {
            return errorResponse(id, -32600, "initialize must not be batched"); // MCP 生命周期要求
        }

        try {
            Object result;
            switch (method) {
                case "initialize":
                    result = initialize(requestMap.get("params"));
                    break;
                case "ping":
                    result = new HashMap<>();
                    break;
                case "tools/list":
                    Map<String, Object> listResult = new HashMap<>();
                    listResult.put("tools", toolSchemas);
                    result = listResult;
                    break;
                case "tools/call":
                    result = call(httpRequest, requestMap.get("params"));
                    break;
                default:
                    return errorResponse(id, -32601, "Method not found");
            }
            return resultResponse(id, result);
        } catch (RpcException e) {
            return errorResponse(id, e.code, e.getMessage(), e.data);
        } catch (Exception e) {
            logger.error("mcp " + method + " failed", e);
            return errorResponse(id, -32603, "Internal error");
        }
    }

    // JSON-RPC 请求契约校验（jsonrpc/method/id/params）；不合法回 -32600，id 非标量时按规范回 null
    private Map<String, Object> invalidRequest(Object node) {
        if (!(node instanceof Map)) return errorResponse(null, -32600, "Invalid Request");

        Map<String, Object> requestMap = (Map<String, Object>) node;
        Object id = requestMap.get("id");
        boolean validId = id == null || id instanceof String || id instanceof Number;
        Object params = requestMap.get("params");
        if (!JSONRPC_VERSION.equals(requestMap.get("jsonrpc"))
                || !(requestMap.get("method") instanceof String)
                || !validId
                || (params != null && !(params instanceof Map) && !(params instanceof List))) {
            return errorResponse(validId ? id : null, -32600, "Invalid Request");
        }
        return null;
    }

    private Map<String, Object> initialize(Object requestParams) {
        if (!(requestParams instanceof Map)) throw new RpcException(-32602, "Invalid params");

        Map<String, Object> params = (Map<String, Object>) requestParams;
        Object version = params.get("protocolVersion");
        if (!(version instanceof String) || ((String) version).isEmpty()
                || !(params.get("capabilities") instanceof Map)
                || !(params.get("clientInfo") instanceof Map)) {
            throw new RpcException(-32602, "Invalid params: initialize");
        }
        return initializeData;
    }

    private void send(HttpResponse httpResponse, Object response) throws Exception {
        httpResponse.contentTypeJsonUtf8().send(json.toJson(response));
    }

    private Map<String, Object> resultResponse(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> errorResponse(Object id, int code, String message) {
        return errorResponse(id, code, message, null);
    }

    private Map<String, Object> errorResponse(Object id, int code, String message, Object data) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) error.put("data", data);

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }

    private Map<String, Object> call(HttpRequest httpRequest, Object requestParams) {
        if (!(requestParams instanceof Map)) throw new RpcException(-32602, "Invalid params");

        Map<String, Object> params = (Map<String, Object>) requestParams;
        Object toolNameObj = params.get("name");
        if (!(toolNameObj instanceof String) || ((String) toolNameObj).isEmpty()) {
            throw new RpcException(-32602, "Invalid params: missing tool name");
        }
        String toolName = (String) toolNameObj;

        Object argumentsObj = params.get("arguments");
        if (argumentsObj != null && !(argumentsObj instanceof Map)) {
            throw new RpcException(-32602, "Invalid params: arguments must be an object");
        }
        Map<String, Object> arguments = argumentsObj == null
                ? Collections.emptyMap() : (Map<String, Object>) argumentsObj; // arguments 缺省即空参

        ToolCache toolCache = toolCacheMap.get(toolName);
        if (toolCache == null) {
            // 未知工具属协议错误，须走 JSON-RPC error 而非 isError（MCP 规范）
            throw new RpcException(-32602, "Unknown tool");
        }

        // 授权：复用共享拦截链；未认证请求已在框架闸门拦截
        String denied = interceptionService.intercept(toolName, arguments, httpRequest.getPrincipal(), httpRequest.getRoles());
        if (denied != null) {
            // 实现自定义错误码区间；data 带机器可读分类，客户端不必解析文案
            throw new RpcException(-32000, denied, Collections.singletonMap("reason", "forbidden"));
        }

        String invokeResult;
        boolean isError = false;
        try {
            invokeResult = toolCache.toolService.invoke(arguments);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断位，避免污染复用的 worker 线程
            throw new RpcException(-32603, "Tool invocation interrupted");
        } catch (Exception e) {
            // 工具业务异常按 MCP 契约以 isError 回传；仅记服务端日志，不回传异常细节
            logger.error("mcp tool invoke failed: " + toolName, e);
            invokeResult = "tool invocation failed";
            isError = true;
        }

        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", invokeResult == null ? "" : invokeResult); // 契约适配：text 必为 string，不输出 null
        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(contentItem);

        Map<String, Object> result = new HashMap<>();
        result.put("content", contentList);
        result.put("isError", isError);
        return result;
    }
}
