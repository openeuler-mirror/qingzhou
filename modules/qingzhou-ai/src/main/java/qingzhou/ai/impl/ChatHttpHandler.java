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
    private VectorStore knowledgeStore;

    private final Map<AiTool, Map<String, Object>> aiTools = new HashMap<>();

    @Activate
    public void init(Map<String, String> config) {
        String knowledgeSystemMessage = null;
        Object knowledge = initKnowledge(config);
        if (knowledge instanceof VectorStore) {
            knowledgeStore = (VectorStore) knowledge;
        } else if (knowledge instanceof String) {
            knowledgeSystemMessage = (String) knowledge;
        }

        chatModel = llm.buildChatModel(
                config.get("chat.base_url"),
                config.get("chat.api_key"),
                config.get("chat.model"),
                Long.parseLong(config.getOrDefault("chat.timeout", "300")),
                Long.parseLong(config.getOrDefault("chat.max_completion_tokens", "5000")),
                knowledgeSystemMessage);
    }

    private Object initKnowledge(Map<String, String> config) {
        List<String> docs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(System.getProperty("qingzhou.version"), "docs"),
                "*.md")) {
            for (Path md : stream) {
                List<String> contents = Files.readAllLines(md);
                if (!contents.isEmpty()) {
                    docs.add(String.join(System.lineSeparator(), contents));
                }
            }
        } catch (Exception e) {
            logger.warn("failed to read knowledge", e);
            return null;
        }
        if (docs.isEmpty()) return null;

        VectorStore knowledgeStore = null;
        String embedUrl = config.get("embed.base_url");
        if (embedUrl != null && !embedUrl.isEmpty()) {
            try {
                EmbeddingModel embeddingModel = llm.buildEmbeddingModel(embedUrl, config.get("embed.api_key"), config.get("embed.model"));
                knowledgeStore = embeddingModel.buildVectorStore();
                for (String doc : docs) {
                    knowledgeStore.insert(doc, 500);
                }
            } catch (Exception e) {
                logger.warn("failed to initialize knowledge store", e);
                knowledgeStore = null;
            }
        }

        return knowledgeStore != null ? knowledgeStore : systemPrompt(docs.toArray(new String[0]), "");
    }

    private String systemPrompt(String[] docs, String question) {
        StringBuilder prompt = new StringBuilder("你是一个轻舟平台的智能助手。请主要依据以下【参考文档】来回答用户的【问题】。" +
                "如果文档中没有相关信息，请明确告知\"文档中未提及\"，并根据已有知识继续进行回答。" +
                "\n\n【参考文档】：");
        for (String doc : docs) {
            prompt.append("\n").append(doc);
        }
        prompt.append("\n\n【问题】：\n").append(question);
        return prompt.toString();
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
        if (knowledgeStore != null) {
            String[] queried = knowledgeStore.query(message);
            if (queried != null) {
                message = systemPrompt(queried, message);
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
            String toolDescription = toolProp.get(AiTool.TOOL_DESCRIPTION).toString();
            String toolName;
            Object toolNameObj = toolProp.get(AiTool.TOOL_NAME);
            if (toolNameObj != null) {
                toolName = (String) toolNameObj;
            } else {
                Object componentName = toolProp.get(ComponentConstants.COMPONENT_NAME);
                if (componentName == null) {
                    throw new IllegalArgumentException("missing parameter [" + AiTool.TOOL_NAME + "] for: " + toolDescription);
                }
                String component = componentName.toString();
                int i = component.lastIndexOf(".");
                toolName = component.substring(i + 1);
            }

            return Tool.of(toolName, toolDescription, parameters(toolProp), toolArgs -> {
                try {
                    return aiTool.invoke(toolArgs);
                } catch (Exception e) {
                    throw new RuntimeException(
                            toolArgs != null ? toolArgs.toString() : e.getMessage(),
                            e);
                }
            });
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
