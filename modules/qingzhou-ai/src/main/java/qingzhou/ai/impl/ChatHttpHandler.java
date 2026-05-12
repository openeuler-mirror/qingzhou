package qingzhou.ai.impl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.AiTool;
import qingzhou.ai.ToolParameter;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Chat;
import qingzhou.llm.LLM;
import qingzhou.llm.Listener;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat",
        configurationPid = "qingzhou-ai", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatHttpHandler implements HttpHandler {
    @Reference
    private LLM llm;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private Chat chat;
    private final Map<AiTool, Map<String, String>> aiTools = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) {
        String baseUrl = config.get("base_url");
        String apiKey = config.get("api_key");
        String model = config.get("model");

        chat = llm.buildChatModel(baseUrl, apiKey, model);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiTool(AiTool tool, Map<String, String> properties) {
        aiTools.put(tool, properties);
    }

    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // 解析请求
        String message = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                Map<String, String> map = json.fromJson(str, HashMap.class);
                message = map.get("message");
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str);
            }
        }
        if (message == null) {
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message("message cannot be null")));
            return;
        }

        // 发出响应
        final String messageId = UUID.randomUUID().toString().replace("-", "");
        httpResponse.contentTypeJsonUtf8();// 返回内容是字符串，非二进制流

        Set<Tool> tools = aiTools.entrySet().stream().map(entry -> {
            AiTool aiTool = entry.getKey();
            Map<String, String> toolProp = entry.getValue();
            String description = toolProp.get(AiTool.TOOL_DESCRIPTION);
            String component = toolProp.get(ComponentConstants.COMPONENT_NAME);
            int i = component.lastIndexOf(".");
            component = component.substring(i + 1);

            ToolParameter[] tp = aiTool.parameters();
            Parameter[] params = tp != null ? convertParams(tp) : null;
            return Tool.of(component, description, params, aiTool::invoke);
        }).collect(Collectors.toSet());
        chat.generate(message, tools, new Listener() {
            boolean isReasoning = false;
            boolean isMessage = false;

            @Override
            public void onBegin() {
                httpResponse.send(resultToString(SseResult.type("RUN_STARTED")));
            }

            @Override
            public void onReasoning(String content) {
                if (!isReasoning) {
                    isReasoning = true;
                    if (isMessage) {
                        httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
                    }
                    isMessage = false;
                    httpResponse.send(resultToString(SseResult.type("REASONING_START")));
                }
                httpResponse.send(resultToString(SseResult.type("REASONING_CONTENT").content(content)));
            }

            @Override
            public void onReasoningPause() {
                httpResponse.send(resultToString(SseResult.type("REASONING_PAUSE")));
            }

            @Override
            public void onReasoningResume() {
                httpResponse.send(resultToString(SseResult.type("REASONING_RESUME")));
            }

            @Override
            public void onToolCall(String toolName, Map<String, Object> args, Object result) {
                if (isReasoning) {
                    httpResponse.send(resultToString(SseResult.type("REASONING_PAUSE")));
                }
                try {
                    httpResponse.send(resultToString(
                            SseResult.type("TOOL_CALL")
                                    .content(json.toJson(args))
                                    .message(json.toJson(result))
                                    .put("toolName", toolName)
                    ));
                } catch (Exception e) {
                    logger.error("Failed to serialize tool call: " + e.getMessage());
                }
                if (isReasoning) {
                    httpResponse.send(resultToString(SseResult.type("REASONING_RESUME")));
                }
            }

            @Override
            public void onMessage(String content) {
                if (!isMessage) {
                    isMessage = true;
                    if (isReasoning) {
                        httpResponse.send(resultToString(SseResult.type("REASONING_END")));
                    }
                    isReasoning = false;
                    httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_START").messageId(messageId)));
                }
                httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_CONTENT").messageId(messageId).content(content)));
            }

            @Override
            public void onError(Throwable t) {
                String errMsg = t.getMessage();
                logger.error(errMsg);
                httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message(errMsg)));
            }

            @Override
            public void onComplete() {
                httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
                httpResponse.sendFinish(resultToString(SseResult.type("RUN_FINISHED")));
            }
        });
    }

    private String resultToString(SseResult result) {
        String toJson;
        try {
            toJson = json.toJson(result.data);
        } catch (Exception e) {
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", result.type, toJson);
    }

    private static Parameter[] convertParams(ToolParameter[] toolParameters) {
        Parameter[] params = new Parameter[toolParameters.length];
        for (int i = 0; i < toolParameters.length; i++) {
            ToolParameter tp = toolParameters[i];
            params[i] = Parameter.of(tp.name(), tp.description(), tp.required(), tp.enumValues());
        }
        return params;
    }
}
