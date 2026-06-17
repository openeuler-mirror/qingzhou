package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class AiHandler implements HttpHandler {
    @Reference
    private ChatModel chatModel;
    @Reference
    private EmbeddingModel embeddingModel;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private VectorStore knowledgeStore;
    private String[] knowledgeDocs;
    private RerankingModel rerankingModel;
    private final Map<AiSkill, Skill> llmSkills = new ConcurrentHashMap<>();

    @Activate
    public void init() {
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
        }
        if (docs.isEmpty()) return;

        if (embeddingModel != null) {
            try {
                knowledgeStore = embeddingModel.buildVectorStore();
                for (String doc : docs) {
                    knowledgeStore.insert(doc, 500);
                }
            } catch (Exception e) {
                logger.warn("failed to initialize knowledge store", e);
                knowledgeStore = null;
            }
        }

        if (knowledgeStore == null) {
            knowledgeDocs = docs.toArray(new String[0]);
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiSkill(AiSkill skill, Map<String, Object> properties) {
        llmSkills.put(skill, new SkillImpl(skill, properties));
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
            message = new String(body, StandardCharsets.UTF_8);
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
