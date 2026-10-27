package qingzhou.ai.mcp;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SkillService;
import qingzhou.ai.impl.InterceptToolInvocation;
import qingzhou.ai.impl.LlmConverter;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;
import qingzhou.logger.Logger;

@Component(property = {HttpHandler.HANDLE_PATH + "=/mcp", HttpHandler.HANDLE_METHODS + "=POST"})
public class McpServer implements HttpHandler {
    private static final String JSONRPC_VERSION = "2.0";

    @Reference
    private Json json;
    @Reference
    private Logger logger;
    @Reference
    private InterceptToolInvocation interceptToolInvocation;

    private final Map<SkillService, Map<String, Object>> llmSkills = new ConcurrentHashMap<>();

    private Map<String, Object> initializeData;

    @Activate
    public void init() {
        initializeData = new HashMap<>();

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "qingzhou-mcp-server");
        serverInfo.put("version", "1.0.0");

        Map<String, Object> capabilities = new HashMap<>();
        Map<String, Object> toolsCapability = new HashMap<>();
        toolsCapability.put("listChanged", false);
        capabilities.put("tools", toolsCapability);

        initializeData.put("protocolVersion", "2025-03-26");
        initializeData.put("serverInfo", serverInfo);
        initializeData.put("capabilities", capabilities);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + SkillService.SKILL_REQUIRED + "=true)")
    public void bindAiSkill(SkillService skill, Map<String, Object> properties) {
        synchronized (this) {
            llmSkills.put(skill, properties);
            toolCache = null; // 技能集变更，使工具缓存失效
        }
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiSkill(SkillService skill) {
        synchronized (this) {
            llmSkills.remove(skill);
            toolCache = null;
        }
    }

    // 工具缓存：llmSkills 仅在 DS bind/unbind 时变更，无需每次请求全量重转；
    // 工具表与 JSON Schema 同置一个不可变快照，避免二者失效时序不一致
    private volatile ToolCache toolCache;

    private static final class ToolCache {
        final Map<String, Tool> tools;
        final List<Map<String, Object>> schemas;

        ToolCache(Map<String, Tool> tools, List<Map<String, Object>> schemas) {
            this.tools = tools;
            this.schemas = schemas;
        }
    }

    private ToolCache toolCache() {
        ToolCache current = toolCache;
        if (current == null) {
            synchronized (this) { // 与 bind/unbind 同一把锁，保证快照基于稳定的技能集
                if (toolCache == null) {
                    toolCache = buildToolCache();
                }
                current = toolCache;
            }
        }
        return current;
    }

    private ToolCache buildToolCache() {
        Set<Tool> tools = LlmConverter.convertSkills(llmSkills, logger).stream()
                .flatMap((Function<qingzhou.llm.Skill, Stream<Tool>>) skill -> skill.tools().stream())
                .collect(Collectors.toSet());
        Map<String, Tool> toolMap = tools.stream().collect(Collectors.toMap(Tool::name, t -> t, (a, b) -> a));
        return new ToolCache(toolMap, toolSchemas(toolMap.values()));
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        byte[] requestBody = httpRequest.getBody();
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
            send(httpResponse, errorResponse(null, -32600, "Invalid Request"));
            return;
        }

        List<Map<String, Object>> replies = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> reply = process(node, httpRequest);
            if (reply != null) replies.add(reply); // notification 不产生响应
        }

        if (replies.isEmpty()) {
            httpResponse.status(202); // 全为 notification：按规范不得回复
            return;
        }
        send(httpResponse, isBatch ? replies : replies.get(0));
    }

    // 单条报文处理；返回 null 表示 notification
    private Map<String, Object> process(Object node, HttpRequest httpRequest) {
        Map<String, Object> invalid = invalidRequest(node);
        if (invalid != null) return invalid;

        Map<String, Object> requestMap = (Map<String, Object>) node;
        if (!requestMap.containsKey("id")) return null; // 如握手必须的 notifications/initialized

        Object id = requestMap.get("id");
        try {
            Object result;
            switch ((String) requestMap.get("method")) {
                case "initialize":
                    result = new HashMap<>(initializeData);
                    break;
                case "ping":
                    result = new HashMap<>();
                    break;
                case "tools/list":
                    Map<String, Object> listResult = new HashMap<>();
                    listResult.put("tools", toolCache().schemas);
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
        }
    }

    // JSON-RPC 请求契约校验（jsonrpc/method/id）；不合法回 -32600，id 非标量时按规范回 null
    private static Map<String, Object> invalidRequest(Object node) {
        if (!(node instanceof Map)) return errorResponse(null, -32600, "Invalid Request");

        Map<String, Object> requestMap = (Map<String, Object>) node;
        Object id = requestMap.get("id");
        boolean validId = id == null || id instanceof String || id instanceof Number;
        Object version = requestMap.get("jsonrpc"); // 兼容省略该成员的客户端，出现即须为 2.0
        if ((version != null && !JSONRPC_VERSION.equals(version))
                || !(requestMap.get("method") instanceof String) || !validId) {
            return errorResponse(validId ? id : null, -32600, "Invalid Request");
        }
        return null;
    }

    private void send(HttpResponse httpResponse, Object response) {
        try {
            httpResponse.contentTypeJsonUtf8().send(json.toJson(response));
        } catch (Exception e) {
            httpResponse.status(500);
            logger.error(e.getMessage(), e);
        }
    }

    private static Map<String, Object> resultResponse(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> errorResponse(Object id, int code, String message) {
        return errorResponse(id, code, message, null);
    }

    private static Map<String, Object> errorResponse(Object id, int code, String message, Object data) {
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

    private static List<Map<String, Object>> toolSchemas(Collection<Tool> tools) {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (Tool tool : tools) {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("name", tool.name());
            toolMap.put("description", tool.description());

            // properties 契约为 JSON Schema object（参数名 → 参数 schema）
            Map<String, Object> properties = new HashMap<>();
            List<String> requiredParameters = new ArrayList<>();
            Parameter[] parameters = tool.parameters();
            if (parameters != null) { // Tool 接口默认实现返回 null
                for (Parameter parameter : parameters) {
                    String type = parameter.type(); // 类型缺省按 string 适配，schema 不接受 null
                    Map<String, Object> parameterSchema = new HashMap<>();
                    parameterSchema.put("type", type == null ? "string" : type);
                    if (parameter.description() != null) { // schema description 须为 string，缺省省略
                        parameterSchema.put("description", parameter.description());
                    }
                    properties.put(parameter.name(), parameterSchema);

                    if (parameter.required()) {
                        requiredParameters.add(parameter.name());
                    }
                }
            }

            Map<String, Object> inputSchema = new HashMap<>();
            inputSchema.put("type", "object");
            inputSchema.put("properties", properties);
            if (!requiredParameters.isEmpty()) { // 空 required 对旧 JSON Schema 校验器非法，缺省省略
                inputSchema.put("required", requiredParameters);
            }
            toolMap.put("inputSchema", inputSchema);

            schemas.add(toolMap);
        }
        return schemas;
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

        // 授权：复用共享拦截链；未认证请求已在框架闸门拦截，principal/roles 非 null
        String denied = interceptToolInvocation.intercept(toolName, arguments, httpRequest.getPrincipal(), httpRequest.getRoles());
        if (denied != null) {
            // 实现自定义错误码区间；data 带机器可读分类，客户端不必解析文案
            throw new RpcException(-32000, denied, Collections.singletonMap("reason", "forbidden"));
        }

        Tool tool = toolCache().tools.get(toolName);
        if (tool == null) {
            // 未知工具属协议错误，须走 JSON-RPC error 而非 isError（MCP 规范）
            throw new RpcException(-32602, "Unknown tool");
        }

        String invokeResult;
        boolean isError = false;
        try {
            invokeResult = tool.invoke(arguments);
        } catch (Throwable e) {
            // 工具内部异常仅记服务端日志，不回传客户端，避免信息泄露
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

    // 协议级错误：上抛后由 process 统一装配为 JSON-RPC error，结果对象不承载错误语义
    private static final class RpcException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int code;
        final Object data;

        RpcException(int code, String message) {
            this(code, message, null);
        }

        RpcException(int code, String message, Object data) {
            super(message);
            this.code = code;
            this.data = data;
        }
    }
}
