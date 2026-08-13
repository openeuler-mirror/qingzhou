package qingzhou.llm.impl.openai;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import qingzhou.llm.Attachment;
import qingzhou.llm.Parameter;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;
import qingzhou.llm.impl.ImageAttachment;

public class OpenAiDialect {
    static Map<String, Object> buildSystemMessage(String systemPrompt, Collection<Skill> skills, List<String> docs) {
        StringBuilder sysMsg = new StringBuilder(systemPrompt != null ? systemPrompt : "");

        if (skills != null) {
            for (Skill skill : skills) {
                String msg = skill.description();
                if (msg != null && !msg.isEmpty()) {
                    if (sysMsg.length() > 0) {
                        sysMsg.append("\n\n");
                    }
                    sysMsg.append("[参考技能]\n").append(msg);
                }
            }
        }

        if (docs != null) {
            for (String doc : docs) {
                if (doc != null && !doc.isEmpty()) {
                    if (sysMsg.length() > 0) {
                        sysMsg.append("\n\n");
                    }
                    sysMsg.append("[参考文档]\n").append(doc);
                }
            }
        }

        Map<String, Object> sysMsgForJson = new HashMap<>();
        sysMsgForJson.put("role", "system");
        sysMsgForJson.put("content", sysMsg.toString());
        return sysMsgForJson;
    }

    static Map<String, Object> buildUserMessage(String message, Attachment[] attachments) {
        List<Object> parts = null;
        if (attachments != null && attachments.length > 0) {
            parts = new ArrayList<>();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", message);
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
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", parts != null ? parts : message);
        return userMsg;
    }

    /**
     * 将流式累积的 ToolCall 转为标准 OpenAI assistant 消息的 tool_calls 结构：
     * [{"id":"...","type":"function","function":{"name":"...","arguments":"..."}}]
     * <p>
     * 注意：不能直接把 ToolCall 对象交给 JSON 序列化——其 package-private 字段会被
     * Jackson 默认忽略（序列化为空对象），导致服务端校验报"工具类型不能为空"。
     */
    static Map<String, Object> buildAssistantMessage(String content, Collection<OpenAiChatModel.ToolCall> toolCalls) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (OpenAiChatModel.ToolCall tc : toolCalls) {
            Map<String, Object> call = new HashMap<>();
            call.put("id", tc.id);
            call.put("type", "function");
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", tc.name);
            fn.put("arguments", tc.arguments != null && !tc.arguments.isEmpty() ? tc.arguments : "{}");
            call.put("function", fn);
            calls.add(call);
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        if (content != null && !content.isEmpty()) {
            msg.put("content", content);
        }
        if (!calls.isEmpty()) {
            msg.put("tool_calls", calls);
        }
        return msg;
    }

    static List<Object> buildToolDefinitions(Collection<Tool> tools) {
        return tools.stream().map((Function<Tool, Object>) tool -> {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");
            Map<String, Object> paramMap = new HashMap<>();
            Parameter[] params = tool.parameters();
            if (params != null) {
                List<String> required = new ArrayList<>();
                for (Parameter param : params) {
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("type", "string");
                    prop.put("description", param.description());
                    paramMap.put(param.name(), prop);
                    if (param.required()) {
                        required.add(param.name());
                    }
                }
                if (!required.isEmpty()) {
                    parameters.put("required", required);
                }
            }
            parameters.put("properties", paramMap);

            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", parameters);

            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", function);
            return toolDef;
        }).collect(Collectors.toList());
    }

    static Map<String, Object> buildLlmRequest(String modelName, List<Object> messages, List<Object> toolDefs) {
        Map<String, Object> req = new HashMap<>();
        req.put("model", modelName);
        req.put("messages", messages);
        req.put("stream", true);
        if (toolDefs != null && !toolDefs.isEmpty()) {
            req.put("tools", toolDefs);
        }
        return req;
    }

    static Map<String, Object> buildToolMessage(String toolId, String result) {
        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "tool");
        toolResultMsg.put("tool_call_id", toolId);
        toolResultMsg.put("content", result);
        return toolResultMsg;
    }
}
