package qingzhou.ai.impl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.*;
import qingzhou.ai.AiTool;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.*;
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
    private final Map<AiTool, Map<String, Object>> aiTools = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) {
        String baseUrl = config.get("base_url");
        String apiKey = config.get("api_key");
        String model = config.get("model");

        chat = llm.buildChatModel(baseUrl, apiKey, model);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
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
                // 在应用里面可包含实例id和应用code等参数
                Map<String, String> map = json.fromJson(str, HashMap.class);
                message = map.entrySet().stream().filter(e -> e.getValue() != null).map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str);
            }
        }
        if (message == null || message.isEmpty()) {
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message("message cannot be null")));
            return;
        }

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        chat.generate(message, tools(), new Listener() {
            final String messageId = UUID.randomUUID().toString().replace("-", "");

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
                try {
                    httpResponse.send(resultToString(
                            SseResult.type("TOOL_CALL")
                                    .content(json.toJson(args))
                                    .message(json.toJson(result))
                                    .toolName(toolName)
                    ));
                } catch (Exception e) {
                    logger.error("Failed to serialize tool call: " + e.getMessage());
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

    private Set<Tool> tools() {
        return aiTools.entrySet().stream().map(entry -> {
            AiTool aiTool = entry.getKey();
            Map<String, Object> toolProp = entry.getValue();
            String description = toolProp.get(AiTool.TOOL_DESCRIPTION).toString();
            String component = toolProp.get(ComponentConstants.COMPONENT_NAME).toString();
            int i = component.lastIndexOf(".");
            component = component.substring(i + 1);

            return Tool.of(component, description, parameters(toolProp), aiTool::invoke);
        }).collect(Collectors.toSet());
    }

    private static Parameter[] parameters(Map<String, Object> toolProp) {
        Map<String, Map<String, String>> params = new LinkedHashMap<>();

        toolProp.forEach((key, value) -> Stream.of(
                AiTool.PARAMETER_NAME, AiTool.PARAMETER_DESCRIPTION, AiTool.PARAMETER_REQUIRED).forEach(flag -> {
            if (key.startsWith(flag)) {
                String sp = "";
                int i = key.indexOf(".");
                if (i != -1) {
                    sp = key.substring(i);
                }
                Map<String, String> param = params.computeIfAbsent(sp, s -> new HashMap<>());
                param.put(flag, (String) value);
            }
        }));

        return params.values().stream()
                .map(map -> Parameter.of(
                        map.get(AiTool.PARAMETER_NAME),
                        map.get(AiTool.PARAMETER_DESCRIPTION),
                        Boolean.parseBoolean(map.getOrDefault(AiTool.PARAMETER_REQUIRED, "true"))))
                .toArray(Parameter[]::new);
    }
}
