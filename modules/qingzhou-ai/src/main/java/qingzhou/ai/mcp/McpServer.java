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

        initializeData.put("protocolVersion", "2024-11-05");
        initializeData.put("serverInfo", serverInfo);
        initializeData.put("capabilities", capabilities);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE,
            target = "(" + SkillService.SKILL_REQUIRED + "=true)")
    public void bindAiSkill(SkillService skill, Map<String, Object> properties) {
        synchronized (this) {
            llmSkills.put(skill, properties);
            cachedToolMap = null; // 技能集变更，使工具缓存失效
        }
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiSkill(SkillService skill) {
        synchronized (this) {
            llmSkills.remove(skill);
            cachedToolMap = null;
        }
    }

    // 技能转换结果缓存：llmSkills 仅在 DS bind/unbind 时变更，无需每次请求全量重转
    private volatile Map<String, Tool> cachedToolMap;

    private Collection<Tool> llmTools() {
        if (cachedToolMap == null) {
            synchronized (this) {
                if (cachedToolMap == null) {
                    Set<Tool> tools = LlmConverter.convertSkills(llmSkills, logger).stream()
                            .flatMap((Function<qingzhou.llm.Skill, Stream<Tool>>) skill -> skill.tools().stream())
                            .collect(Collectors.toSet());
                    cachedToolMap = tools.stream().collect(Collectors.toMap(Tool::name, t -> t, (a, b) -> a));
                }
            }
        }
        return cachedToolMap.values();
    }

    private Tool findTool(String toolName) {
        llmTools(); // 首个请求即 tools/call 时缓存尚未建立，先触发懒加载
        Map<String, Tool> toolMap = cachedToolMap;
        return toolMap == null ? null : toolMap.get(toolName);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // 解析JSON请求
        byte[] requestBody = httpRequest.getBody();
        String body = requestBody == null ? "" : new String(requestBody, StandardCharsets.UTF_8);
        Map<String, Object> requestMap;
        try {
            requestMap = json.fromJson(body, HashMap.class);
        } catch (Exception e) {
            httpResponse.status(400).finish();
            return;
        }
        if (requestMap == null) {
            httpResponse.status(400).finish();
            return;
        }

        // 响应信封
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", requestMap.get("id"));

        String requestMethod = (String) requestMap.get("method");
        if ("initialize".equals(requestMethod)) {
            response.put("result", new HashMap<>(initializeData));
        } else if ("tools/list".equals(requestMethod)) {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("tools", tools());
            response.put("result", resultData);
        } else if ("tools/call".equals(requestMethod)) {
            response.putAll(call(httpRequest, requestMap.get("params")));
        } else {
            response.put("error", errorBody(-32601, "Method not found"));
        }

        // 响应
        try {
            httpResponse.contentTypeJsonUtf8()
                    .send(json.toJson(response)).finish();
        } catch (Exception e) {
            httpResponse.status(500).finish();
            logger.error(e.getMessage(), e);
        }
    }

    private static Map<String, Object> errorBody(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private Object tools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        Collection<Tool> llmTools = llmTools();
        llmTools.forEach(tool -> {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("name", tool.name());
            toolMap.put("description", tool.description());

            List<Map<String, Object>> toolProperties = new ArrayList<>();
            List<String> requiredParameters = new ArrayList<>();
            Parameter[] parameters = tool.parameters();
            if (parameters != null) {
                for (Parameter parameter : parameters) {
                    Map<String, Object> parameterMap = new HashMap<>();
                    Map<String, Object> textParam = new HashMap<>();
                    textParam.put("type", "string");
                    textParam.put("description", parameter.description());
                    parameterMap.put(parameter.name(), textParam);
                    toolProperties.add(parameterMap);

                    if (parameter.required()) {
                        requiredParameters.add(parameter.name());
                    }
                }
            }

            Map<String, Object> toolInputSchema = new HashMap<>();
            toolInputSchema.put("type", "object");
            toolInputSchema.put("properties", toolProperties);
            toolInputSchema.put("required", requiredParameters);
            toolMap.put("inputSchema", toolInputSchema);

            tools.add(toolMap);
        });

        return tools;
    }

    private Map<String, Object> call(HttpRequest httpRequest, Object requestParams) {
        Map<String, Object> response = new HashMap<>();

        if (!(requestParams instanceof Map)) {
            response.put("error", errorBody(-32602, "Invalid params"));
            return response;
        }
        Map<String, Object> params = (Map<String, Object>) requestParams;

        Object toolNameObj = params.get("name");
        if (!(toolNameObj instanceof String) || ((String) toolNameObj).isEmpty()) {
            response.put("error", errorBody(-32602, "Invalid params: missing tool name"));
            return response;
        }
        String toolName = (String) toolNameObj;

        Object argumentsObj = params.get("arguments");
        Map<String, Object> arguments = argumentsObj instanceof Map
                ? (Map<String, Object>) argumentsObj : Collections.emptyMap();

        // 授权：复用共享拦截链；未认证请求已在框架闸门拦截，principal/roles 非 null
        String denied = interceptToolInvocation.intercept(toolName, arguments, httpRequest.getPrincipal(), httpRequest.getRoles());
        if (denied != null) {
            response.put("error", errorBody(-32000, denied));
            return response;
        }

        String invokeResult;
        boolean isError = true;
        try {
            Tool tool = findTool(toolName);
            if (tool != null) {
                invokeResult = tool.invoke(arguments);
                isError = false;
            } else {
                invokeResult = "Not Found Tool";
            }
        } catch (Throwable e) {
            // 工具内部异常仅记服务端日志，不回传客户端，避免信息泄露
            logger.error("mcp tool invoke failed: " + toolName, e);
            invokeResult = "tool invocation failed";
        }

        List<Map<String, Object>> contentList = new ArrayList<>();
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", invokeResult);
        contentList.add(contentItem);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("content", contentList);
        resultData.put("isError", isError);
        response.put("result", resultData);
        return response;
    }
}
