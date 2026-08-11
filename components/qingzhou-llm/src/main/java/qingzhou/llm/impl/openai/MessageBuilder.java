package qingzhou.llm.impl.openai;

import java.util.*;

import qingzhou.llm.Attachment;
import qingzhou.llm.Skill;
import qingzhou.llm.impl.ImageAttachment;

class MessageBuilder {
    private final OpenAiChatModelBuilder builder;

    MessageBuilder(OpenAiChatModelBuilder builder) {
        this.builder = builder;
    }

    Map<String, Object> buildSystemMessage() {
        String system = builder.systemPrompt;
        if (builder.skills != null) {
            StringBuilder sb = new StringBuilder(builder.systemPrompt);
            for (Skill skill : builder.skills) {
                String instruction = skill.instruction();
                if (instruction != null && !instruction.isEmpty()) {
                    sb.append("\n\n").append(instruction);
                }
            }
            system = sb.toString();
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "system");
        msg.put("content", system);
        return msg;
    }

    Map<String, Object> buildUserMessage(String message, Attachment[] attachments) {
        String content = message;
        if (builder.docs != null && !builder.docs.isEmpty()) {
            String sp = "\n\n[参考附件]\n";
            content += sp + String.join(sp, builder.docs);
        }

        if (attachments != null && attachments.length > 0) {
            List<Object> parts = new ArrayList<>();
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", content);
            parts.add(textPart);

            for (Attachment attach : attachments) {
                if (attach instanceof ImageAttachment) {
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("type", "image_url");
                    Map<String, Object> imageUrl = new HashMap<>();
                    imageUrl.put("url", "data:image/jpeg;base64," + ((ImageAttachment) attach).base64);
                    imagePart.put("image_url", imageUrl);
                    parts.add(imagePart);
                }
            }

            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", parts);
            return msg;
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", content);
        return msg;
    }
}
