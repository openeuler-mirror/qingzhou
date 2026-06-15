package qingzhou.ai.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.AiSkill;
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

    private final String aiDocDir = new File(System.getProperty("qingzhou.version"), "ai").getAbsolutePath();

    private ChatModel chatModel;
    private VectorStore knowledgeStore;
    private String[] knowledgeDocs;
    private RerankingModel rerankingModel;
    private final Map<AiSkill, Skill> llmSkills = new ConcurrentHashMap<>();

    @Activate
    public void init(Map<String, String> config) throws IOException {
        // rag 检索
        Object knowledge = initKnowledge(config);
        if (knowledge instanceof VectorStore) {
            knowledgeStore = (VectorStore) knowledge;
        } else if (knowledge instanceof String[]) {
            knowledgeDocs = (String[]) knowledge;
        } else {
            throw new IllegalStateException(String.valueOf(knowledge));
        }

        // rag 优化排序
        String rerankUrl = config.get("rerank.base_url");
        if (rerankUrl != null && !rerankUrl.isEmpty()) {
            rerankingModel = llm.buildRerankingModel(rerankUrl, config.get("rerank.api_key"),
                    config.get("rerank.model"));
        }

        String systemPrompt = String.join("\n", Files.readAllLines(
                Paths.get(aiDocDir, "system_prompt.md"),
                StandardCharsets.UTF_8));

        // chat 大模型
        chatModel = llm.buildChatModel(
                config.get("chat.base_url"),
                config.get("chat.api_key"),
                config.get("chat.model"),
                Long.parseLong(config.getOrDefault("chat.timeout", "60")),
                systemPrompt,
                null);
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

        return knowledgeStore != null ? knowledgeStore : docs.toArray(new String[0]);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiSkill(AiSkill skill, Map<String, Object> properties) {
        llmSkills.put(skill, new SkillImpl(skill, properties, aiDocDir));
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiSkill(AiSkill skill) {
        llmSkills.remove(skill);
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
        String[] refDocs;
        if (knowledgeStore != null) {
            refDocs = knowledgeStore.query(message);
        } else {
            refDocs = knowledgeDocs;
        }
        if (refDocs != null && rerankingModel != null) {
            refDocs = rerankingModel.rerank(message, refDocs);
        }

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        chatModel.chat(message, refDocs,
                null,
                llmSkills.values(),
                new SseListener(httpResponse, logger, json));
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
