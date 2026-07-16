package qingzhou.ai.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.AiSkill;
import qingzhou.ai.LlmConverter;
import qingzhou.ai.SystemAiTool;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModel;
import qingzhou.llm.ChatModelFactory;
import qingzhou.llm.Skill;
import qingzhou.logger.Logger;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat")
public class AiChat implements HttpHandler {
    @Reference
    private ChatModelFactory chatModelFactory;

    @Reference
    private AiEquip aiEquip;

    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private final Map<SystemAiTool, Map<String, Object>> systemAiTools = new ConcurrentHashMap<>();

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindSystemAiTool(SystemAiTool tool, Map<String, Object> properties) {
        systemAiTools.put(tool, properties);
    }

    // OSGI 框架根据名称规则自动识别调用此方法或在子类的 @Reference 中指定
    public void unbindSystemAiTool(SystemAiTool tool) {
        systemAiTools.remove(tool);
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

        Skill llmSkill = null;
        List<String> refDocs = null;
        Attachment[] images = null;
        String skillName = (String) params.get("skill");
        if (skillName != null) {
            for (Map.Entry<AiSkill, Skill> entry : aiEquip.llmSkills.entrySet()) {
                AiSkill aiSkill = entry.getKey();
                Skill skill = entry.getValue();
                if (skill.name().equals(skillName)) {
                    llmSkill = skill;
                    Map<AiSkill.AttachmentType, String[]> attachmentTypeMap = aiSkill.supportedAttachmentTypes();
                    if (attachmentTypeMap != null) {
                        for (AiSkill.AttachmentType attachmentType : attachmentTypeMap.keySet()) {
                            List<String> attachments = findAttachments(params, attachmentType);
                            switch (attachmentType) {
                                case text:
                                    refDocs = attachments;
                                    break;
                                case image:
                                    images = attachments.stream().map(s -> chatModelFactory.buildImageAttachment(s)).toArray(Attachment[]::new);
                                    break;
                                default:
                                    logger.warn("unsupported type: " + attachmentType);
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 放在最后
        String app = (String) params.get("app");
        if (app != null && !app.isEmpty()) {
            question = ("在“" + app + "”应用范围内，回复：" + question);
        }
        // 发出响应
        httpResponse.contentType("text/event-stream; charset=utf-8")
                .header("connection", "keep-alive")
                .header("cache-control", "no-cache");
        ChatModel chatModel = chatModelFactory.newChatModelBuilder()
                .withDoc(refDocs)
                .withTool(LlmConverter.convertSystemAiTool(systemAiTools))
                .withSkill(llmSkill == null ? null : Collections.singleton(llmSkill))
                .build();
        chatModel.chat(question, new SseListener(httpResponse, logger, json), images);
    }

    private List<String> findAttachments(Map<String, Object> params, AiSkill.AttachmentType expectedType) {
        List<String> found = new ArrayList<>();
        Object attachments = params.get("attachments");
        if (attachments instanceof List) {
            for (Object item : (List<?>) attachments) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) item;
                String type = map.get("type") == null ? null : String.valueOf(map.get("type"));
                String content = map.get("content") == null ? null : String.valueOf(map.get("content"));
                if (content == null || content.isEmpty()) continue;
                if (Objects.equals(type, expectedType.name())) {
                    found.add(content);
                }
            }
        }
        return found;
    }
}
