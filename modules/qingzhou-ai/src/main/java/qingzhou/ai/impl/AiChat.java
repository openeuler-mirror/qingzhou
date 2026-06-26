package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.SystemAiTool;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat")
public class AiChat implements HttpHandler {
    @Reference
    private ChatModelFactory chatModelFactory;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private EmbeddingModel embeddingModel;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private RerankingModel rerankingModel;

    @Reference
    private AiEquip aiEquip;

    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private VectorStore knowledgeStore;
    private String[] knowledgeDocs;

    private final Map<SystemAiTool, Map<String, Object>> systemAiTools = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindSystemAiTool(SystemAiTool tool, Map<String, Object> properties) {
        systemAiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindSystemAiTool(SystemAiTool tool) {
        systemAiTools.remove(tool);
    }

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

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        // 解析请求
        String question = null;
        String skills = null;
        String attachments = null;
        String apps = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                // 在应用里面可包含实例id和应用code等参数
                Map<String, String> map = json.fromJson(str, HashMap.class);
                question = map.get("question");
                apps = map.get("apps");
                skills = map.get("skills");
                attachments = map.get("attachments");
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str);
            }
        }
        if (question == null || question.isEmpty()) {
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message("message cannot be null"), json));
            return;
        }
        if (apps != null && !apps.isEmpty()) {
            question = ("在“" + apps + "”应用内，回复：" + question);
        }

        // RAG 检索增强问答
        String[] refDocs;
        if (knowledgeStore != null) {
            refDocs = knowledgeStore.query(question);
        } else {
            refDocs = knowledgeDocs;
        }
        if (refDocs != null && rerankingModel != null) {
            refDocs = rerankingModel.rerank(question, refDocs);
        }

        // 添加自定义技能
        List<Skill> skillList = new ArrayList<>();
        if (skills != null && !skills.isEmpty()) {
            String finalSkills = skills;
            skillList = aiEquip.llmSkills.values().stream().filter(skill -> Arrays.stream(finalSkills.split(",")).anyMatch(s -> skill.name().equals(s))).collect(Collectors.toList());
        }

        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");


        List<Attachment> attachmentList = new ArrayList<>();
        if (attachments != null && !attachments.isEmpty()) {
            for (String attach : attachments.split(",")) {
                attachmentList.add(chatModelFactory.buildImageAttachment(attach));
            }
        }

        ChatModel chatModel = chatModelFactory.newChatModelBuilder()
                .withDoc(refDocs)
                .withTool(Converter.convertSystemAiTool(systemAiTools))
                .withSkill(skillList)
                .build();
        chatModel.chat(question, new SseListener(httpResponse, logger, json), attachmentList.toArray(new Attachment[0]));
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
