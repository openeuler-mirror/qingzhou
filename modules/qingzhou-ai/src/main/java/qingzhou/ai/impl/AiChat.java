package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.*;
import qingzhou.ai.Converter;
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
    private List<String> knowledgeDocs;

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
            knowledgeDocs = docs;
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        Map<String, Object> params = null;
        String question = null;
        byte[] body = httpRequest.getBody();
        if (body != null && body.length > 0) {
            String str = new String(body, StandardCharsets.UTF_8);
            try {
                params = json.fromJson(str, HashMap.class);
                question = (String) params.get("question");
            } catch (Exception e) {
                logger.error("failed to convert to JSON: " + str, e);
            }
        }
        if (question == null || question.trim().isEmpty()) {
            logger.error("message is null");
            return;
        }

        List<String> refDocs = parseRefDocs(params, question);
        Skill skill = parseSkill(params);
        Attachment[] images = findAttachments(params, "image").stream().map(s -> chatModelFactory.buildImageAttachment(s)).toArray(Attachment[]::new);
        // 放在最后
        String app = (String) params.get("app");
        if (app != null && !app.isEmpty()) {
            question = ("在“" + app + "”应用内，回复：" + question);
        }
        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        if (rerankingModel != null) {
            try {
                refDocs = rerankingModel.rerank(question, refDocs);
            } catch (Throwable e) { // 误打开配置文件里的注释后，若配置为空也会报错
                logger.warn("failed to load the re-ranking model", e);
            }
        }
        ChatModel chatModel = chatModelFactory.newChatModelBuilder()
                .withDoc(refDocs)
                .withTool(Converter.convertSystemAiTool(systemAiTools))
                .withSkill(Collections.singleton(skill))
                .build();
        chatModel.chat(question, new SseListener(httpResponse, logger, json), images);
    }

    private List<String> parseRefDocs(Map<String, Object> params, String question) throws IOException {
        // RAG 检索增强问答
        List<String> refDocs = new ArrayList<>();
        if (knowledgeStore != null) {
            refDocs.addAll(knowledgeStore.query(question));
        } else {
            refDocs.addAll(knowledgeDocs);
        }

        List<String> textList = findAttachments(params, "text");
        refDocs.addAll(textList);

        return refDocs;
    }

    private Skill parseSkill(Map<String, Object> params) {
        String skill = (String) params.get("skill");
        if (skill != null && !skill.isEmpty()) {
            for (Skill value : aiEquip.llmSkills.values()) {
                if (value.name().equals(skill)) return value;
            }
        }
        return null;
    }

    private List<String> findAttachments(Map<String, Object> params, String expectedType) {
        List<String> found = new ArrayList<>();
        Object attachments = params.get("attachments");
        if (attachments instanceof List) {
            for (Object item : (List<?>) attachments) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) item;
                String type = map.get("type") == null ? null : String.valueOf(map.get("type"));
                String content = map.get("content") == null ? null : String.valueOf(map.get("content"));
                if (content == null || content.isEmpty()) continue;
                if (Objects.equals(type, expectedType)) {
                    found.add(content);
                }
            }
        }
        return found;
    }
}
