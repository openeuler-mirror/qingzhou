package qingzhou.llm.impl.openai;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import qingzhou.json.Json;
import qingzhou.llm.*;
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

    static Map<String, Object> buildAssistantMessage(String content, Object toolCalls) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        if (content != null && !content.isEmpty()) {
            msg.put("content", content);
        }
        if (toolCalls != null) {
            msg.put("tool_calls", toolCalls);
        }
        return msg;
    }

    static List<Object> buildToolDefinition(Collection<Tool> tools) {
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

    static Map<String, Object> callTool(Map<String, Object> llmToolCall, Listener listener, Map<String, Tool> tools, Json json) {
        Map<String, Object> function = (Map<String, Object>) llmToolCall.get("function");
        String toolName = (String) function.get("name");
        String toolArgs = (String) function.get("arguments");

        String result;
        Tool tool = tools.get(toolName);
        if (tool != null) {
            Map<String, Object> args = null;
            if (toolArgs != null && !toolArgs.isEmpty()) {
                try {
                    args = json.fromJson(toolArgs, Map.class);
                } catch (Exception ignored) {
                }
            }
            try {
                listener.onToolCall(tool.name());
                result = tool.invoke(args);
            } catch (Throwable t) {
                result = "Error: " + t.getMessage();
            }
        } else {
            result = "Tool not found: " + toolName;
        }

        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "tool");
        toolResultMsg.put("tool_call_id", llmToolCall.get("id"));
        toolResultMsg.put("content", result);
        return toolResultMsg;
    }
}
