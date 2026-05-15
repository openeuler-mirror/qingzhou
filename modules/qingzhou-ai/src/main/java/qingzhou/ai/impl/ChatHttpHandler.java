package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private ChatModel chatModel;
    private VectorStore knowledgeBase;

    private final Map<AiTool, Map<String, Object>> aiTools = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) throws IOException {
        initKnowledgeBase(config);

        chatModel = llm.buildChatModel(
                config.get("chat.base_url"),
                config.get("chat.api_key"),
                config.get("chat.model"),
                knowledgeBase == null ? buildKnowledgeSystemMessage() : null);
    }

    private void initKnowledgeBase(Map<String, String> config) {
        String embedUrl = config.get("embed.base_url");
        if (embedUrl != null && !embedUrl.isEmpty()) {
            Path docsDir = Paths.get(System.getProperty("qingzhou.version"), "docs");
            if (Files.exists(docsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.md")) {
                    for (Path md : stream) {
                        List<String> contents = Files.readAllLines(md);
                        if (!contents.isEmpty()) {
                            if (knowledgeBase == null) {
                                EmbeddingModel embeddingModel = llm.buildEmbeddingModel(embedUrl, config.get("embed.api_key"), config.get("embed.model"));
                                knowledgeBase = embeddingModel.buildVectorStore();
                            }
                            knowledgeBase.insert(String.join(System.lineSeparator(), contents), 500);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("failed to initialize knowledge", e);
                    knowledgeBase = null;
                }
            }
        }
    }

    private String buildKnowledgeSystemMessage() throws IOException {
        StringBuilder systemPrompt = new StringBuilder("你是一个轻舟平台的智能助手。请主要依据以下【参考文档】来回答用户的【问题】。" +
                "如果文档中没有相关信息，请明确回答\"文档中未提及\"，绝不要自行编造。【参考文档】：");

        // 使用通配符过滤，只要 .md 文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(System.getProperty("qingzhou.version"), "docs"),
                "*.md")) {
            for (Path md : stream) {
                systemPrompt.append(System.lineSeparator());
                List<String> contents = Files.readAllLines(md);
                contents.forEach(str -> systemPrompt.append(System.lineSeparator()).append(str));
            }
        }
        return systemPrompt.toString();
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiTool(AiTool tool, Map<String, Object> properties) {
        aiTools.put(tool, properties);
    }

    public void unbindAiTool(AiTool tool) {
        aiTools.remove(tool);
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
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
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message("message cannot be null"), json));
            return;
        }

        // RAG 检索增强问答
        if (knowledgeBase != null) {
            String[] queried = knowledgeBase.query(message);
            if (queried != null) {
                StringBuilder ragPrompt = new StringBuilder("你是一个轻舟平台的智能助手。请主要依据以下【参考文档】来回答用户的【问题】。" +
                        "如果文档中没有相关信息，请明确回答\"文档中未提及\"，绝不要自行编造。【参考文档】：");
                for (String s : queried) {
                    ragPrompt.append(s).append(System.lineSeparator());
                }
                ragPrompt.append("【问题】：").append(message);
                message = ragPrompt.toString();
            }
        }

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        chatModel.chat(message, tools(), new SseListener(httpResponse, logger, json));
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

    private Parameter[] parameters(Map<String, Object> toolProp) {
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

    static String resultToString(SseResult result, Json json) {
        String toJson;
        try {
            toJson = json.toJson(result.data);
        } catch (Exception e) {
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", result.type, toJson);
    }
}
