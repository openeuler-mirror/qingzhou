package qingzhou.ai.mcp;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.ai.ToolCallInterceptionService;
import qingzhou.http.server.HttpRequest;
import qingzhou.logger.Logger;

/**
 * MCP 方法语义：initialize、ping、tools/list、tools/call。
 * 参数校验属网络输入边界，在此一次性完成；协议错误抛 {@link RpcException}，由传输层装配成 error 报文。
 */
class McpMethods {
    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final ToolRegistry registry;
    private final ToolCallInterceptionService interceptionService;
    private final Logger logger;
    private final Map<String, Object> initializeResult;

    McpMethods(ToolRegistry registry, ToolCallInterceptionService interceptionService, Logger logger, String version) {
        this.registry = registry;
        this.interceptionService = interceptionService;
        this.logger = logger;

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "qingzhou-mcp-server");
        serverInfo.put("version", version);

        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("serverInfo", serverInfo);
        result.put("capabilities", Collections.singletonMap("tools", Collections.singletonMap("listChanged", false)));
        initializeResult = Collections.unmodifiableMap(result);
    }

    Object invoke(String method, Object params, HttpRequest request) throws RpcException {
        switch (method) {
            case "initialize":
                return initializeResult;
            case "ping":
                return Collections.emptyMap();
            case "tools/list":
                return Collections.singletonMap("tools", registry.schemas());
            case "tools/call":
                return callTool(params, request);
            default:
                throw new RpcException(RpcException.METHOD_NOT_FOUND, "Method not found");
        }
    }

    @SuppressWarnings("unchecked") // params 来自 JSON 对象
    private Object callTool(Object requestParams, HttpRequest request) throws RpcException {
        if (!(requestParams instanceof Map)) throw new RpcException(RpcException.INVALID_PARAMS, "Invalid params");

        Map<String, Object> params = (Map<String, Object>) requestParams;
        Object toolName = params.get("name");
        if (!(toolName instanceof String) || ((String) toolName).isEmpty()) {
            throw new RpcException(RpcException.INVALID_PARAMS, "Invalid params: missing tool name");
        }

        ToolCache toolCache = registry.find((String) toolName);
        if (toolCache == null) {
            // 未知工具属协议错误：须走 JSON-RPC error 而非 isError（MCP 规范）
            throw new RpcException(RpcException.INVALID_PARAMS, "Unknown tool");
        }
        return invokeTool(toolCache, params.get("arguments"), request);
    }

    private Map<String, Object> invokeTool(ToolCache toolCache, Object arguments, HttpRequest request) throws RpcException {
        Map<String, Object> toolArgs = normalizeArguments(arguments);

        // 授权：所有工具调用必须过共享拦截链；未认证请求已在框架闸门拦截
        String denied = interceptionService.intercept(toolCache.toolName, toolArgs, request.getPrincipal(), request.getRoles());
        if (denied != null) {
            // data 带机器可读分类，客户端不必解析文案
            throw new RpcException(RpcException.FORBIDDEN, denied, Collections.singletonMap("reason", "forbidden"));
        }

        String result;
        boolean isError = false;
        try {
            result = toolCache.toolService.invoke(toolArgs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断位，避免污染复用的 worker 线程
            throw new RpcException(RpcException.INTERNAL_ERROR, "Tool invocation interrupted");
        } catch (Exception e) {
            // 工具业务异常按 MCP 契约以 isError 回传；细节只进服务端日志
            logger.error("mcp tool invoke failed: " + toolCache.toolName, e);
            result = "tool invocation failed";
            isError = true;
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "text");
        content.put("text", result == null ? "" : result); // 契约适配：text 必为 string，不输出 null

        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("content", Collections.singletonList(content));
        reply.put("isError", isError);
        return reply;
    }

    // 契约适配：inputSchema 参数声明为 string，标量统一转字符串，避免工具侧 (String) 强转失败
    @SuppressWarnings("unchecked") // JSON 对象键必为 String
    private static Map<String, Object> normalizeArguments(Object arguments) throws RpcException {
        if (arguments == null) return Collections.emptyMap(); // arguments 缺省即空参
        if (!(arguments instanceof Map)) {
            throw new RpcException(RpcException.INVALID_PARAMS, "Invalid params: arguments must be an object");
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        ((Map<String, Object>) arguments).forEach((name, value) -> normalized.put(name,
                value instanceof Number || value instanceof Boolean || value instanceof Character
                        ? String.valueOf(value) : value));
        return normalized;
    }
}
