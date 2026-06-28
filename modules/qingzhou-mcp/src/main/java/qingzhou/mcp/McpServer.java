package qingzhou.mcp;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.Converter;
import qingzhou.ai.SystemAiTool;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=")
public class McpServer implements HttpHandler {
    @Reference
    private Json json;

    @Reference
    private Logger logger;

    private final Map<SystemAiTool, Map<String, Object>> systemAiTools = new ConcurrentHashMap<>();
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

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindSystemAiTool(SystemAiTool tool, Map<String, Object> properties) {
        systemAiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindSystemAiTool(SystemAiTool tool) {
        systemAiTools.remove(tool);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        // 解析JSON请求
        String body = new String(httpRequest.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> requestMap = json.fromJson(body, HashMap.class);

        // 响应对象
        Map<String, Object> result = new HashMap<>();
        result.put("jsonrpc", "2.0");
        result.put("id", requestMap.get("id"));
        Map<String, Object> resultData = new HashMap<>();
        result.put("result", resultData);

        String requestMethod = (String) requestMap.get("method");
        if ("initialize".equals(requestMethod)) {
            resultData.putAll(initializeData);
        } else if ("tools/list".equals(requestMethod)) {
            resultData.put("tools", tools());
        } else if ("tools/call".equals(requestMethod)) {
            resultData.putAll(call(requestMap.get("params")));
        }

        // 响应
        httpResponse.contentTypeJsonUtf8()
                .sendFinish(json.toJson(result));
    }

    private Object tools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        Collection<Tool> llmTools = Converter.convertSystemAiTool(systemAiTools);
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

    private Map<String, Object> call(Object requestParams) {
        Map<String, Object> params = (Map<String, Object>) requestParams;
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        boolean isError = true;
        String invokeResult = null;
        try {
            boolean found = false;
            Collection<Tool> llmTools = Converter.convertSystemAiTool(systemAiTools);
            for (Tool tool : llmTools) {
                if (tool.name().equals(toolName)) {
                    invokeResult = tool.invoke(arguments);
                    isError = false;
                    found = true;
                    break;
                }
            }
            if (!found) {
                invokeResult = "Not Found Tool";
            }
        } catch (Throwable e) {
            invokeResult = e.getMessage();
            logger.error(e.getMessage(), e);
        }

        List<Map<String, Object>> contentList = new ArrayList<>();
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", invokeResult);
        contentList.add(contentItem);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("content", contentList);
        resultData.put("isError", isError);
        return resultData;
    }
}
