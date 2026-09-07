package qingzhou.llm.impl.openai;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.llm.impl.ChatModelBuilderBase;
import qingzhou.llm.impl.ImageAttachment;
import qingzhou.llm.impl.ToolCallInfo;
import qingzhou.llm.impl.Utils;
import qingzhou.llm.openai.ImageDetail;
import qingzhou.llm.openai.OpenAiDialect;
import qingzhou.llm.openai.ReasoningEffort;

public class OpenAiChatModelBuilder extends ChatModelBuilderBase implements OpenAiDialect {
    private final HttpClient httpClient;
    private final Json json;

    public ReasoningEffort effort;
    public ImageDetail imageDetail;
    public Map<String, String> format;

    private String reasoningField;

    public OpenAiChatModelBuilder(String baseUrl, String apiKey, String model, HttpClient httpClient, Json json) {
        super(baseUrl, apiKey, model);

        this.httpClient = httpClient;
        this.json = json;
    }

    @Override
    protected ChatModel buildInternal() {
        return new OpenAiChatModel(this, httpClient, json);
    }

    @Override
    public ChatModelFactory.ChatModelBuilder enableThinking(boolean enableThinking) {
        checkSealed();
        if (this.effort == null || this.effort == ReasoningEffort.none) {
            this.effort = ReasoningEffort.medium;
        }
        return this;
    }

    @Override
    public LlmDialect getLlmDialect() {
        checkSealed();
        return this;
    }

    @Override
    public OpenAiDialect reasoningEffort(ReasoningEffort effort) {
        checkSealed();
        this.effort = effort;
        return this;
    }

    @Override
    public OpenAiDialect imageDetail(ImageDetail imageDetail) {
        checkSealed();
        this.imageDetail = imageDetail;
        return this;
    }

    @Override
    public OpenAiDialect responseFormat(Map<String, String> format) {
        checkSealed();
        this.format = format;
        return this;
    }

    Map<String, Object> buildLlmRequest(List<Object> messages, List<Object> toolDefs, boolean stream) {
        Map<String, Object> req = new HashMap<>();
        req.put("model", model);
        req.put("messages", messages);
        req.put("stream", stream);
        if (effort != null) {
            req.put("reasoning_effort", effort.name());
        }
        if (format != null) {
            req.put("response_format", format);
        }
        if (stream) {
            // 请求流式响应末尾附带 usage 统计（最后一个 chunk 的 choices 为空、携带 usage 字段）
            Map<String, Object> streamOptions = new HashMap<>();
            streamOptions.put("include_usage", true);
            req.put("stream_options", streamOptions);
        }
        if (toolDefs != null && !toolDefs.isEmpty()) {
            req.put("tools", toolDefs);
        }
        return req;
    }

    List<Object> buildToolDefinitions(Collection<Tool> tools) {
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
                    if (param.description() != null) {
                        prop.put("description", param.description());
                    }
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
            if (tool.description() != null) {
                function.put("description", tool.description());
            }
            function.put("parameters", parameters);

            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", function);
            return toolDef;
        }).collect(Collectors.toList());
    }

    /**
     * 思考内容的字段名没有统一标准：OpenAI 的 Chat Completions 规范里根本没有该字段，
     * 各家都是自行扩展，且还在演进——vLLM 已把 reasoning_content 改名为 reasoning
     * （见 https://docs.vllm.ai/en/latest/features/reasoning_outputs/），
     * 而 DeepSeek 及旧版本仍是 reasoning_content。因此这里两个都认。
     */
    String extractReasoning(Map<String, Object> delta) {
        Object value = delta.get("reasoning");
        if (value != null) {
            reasoningField = "reasoning";
        } else {
            value = delta.get("reasoning_content");
            if (value != null) {
                reasoningField = "reasoning_content";
            }
        }

        return value != null ? value.toString() : "";
    }

    /**
     * 将流式累积的 ToolCall 转为标准 OpenAI assistant 消息的 tool_calls 结构：
     * [{"id":"...","type":"function","function":{"name":"...","arguments":"..."}}]
     * <p>
     * 注意：不能直接把 ToolCall 对象交给 JSON 序列化——其 package-private 字段会被
     * Jackson 默认忽略（序列化为空对象），导致服务端校验报"工具类型不能为空"。
     * <p>
     * reasoningField 本轮服务端实际使用的思考字段名（reasoning / reasoning_content）。
     * 思考内容需按原字段名回传，否则部分服务端（如 deepseek）会校验失败。
     */
    Map<String, Object> buildAssistantMessage(String content, String reasoning, Collection<ToolCallInfo> toolCalls) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (ToolCallInfo tc : toolCalls) {
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
        msg.put("content", content);

        if (reasoningField != null && reasoning != null && !reasoning.isEmpty()) {
            // 用服务端本轮实际返回的字段名回传；未收到思考内容时沿用 reasoning_content（空串），保持向后兼容
            msg.put(reasoningField, reasoning);
        }

        if (!calls.isEmpty()) {
            msg.put("tool_calls", calls);
        }
        return msg;
    }

    Map<String, Object> buildToolMessage(String toolId, String result) {
        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "tool");
        toolResultMsg.put("tool_call_id", toolId);
        toolResultMsg.put("content", Utils.truncate(result, maxToolResultChars));
        return toolResultMsg;
    }

    Map<String, Object> buildSystemMessage(Collection<Skill> activeSkills) {
        StringBuilder sysMsg = new StringBuilder(systemPrompt != null ? systemPrompt : "");

        for (Skill skill : activeSkills) {
            String msg = skill.instruction();
            if (msg != null && !msg.isEmpty()) {
                if (sysMsg.length() > 0) {
                    sysMsg.append("\n\n");
                }
                sysMsg.append("[技能：").append(skill.name()).append("]\n").append(Utils.truncate(msg, maxPerRefChars));
            }
        }

        if (docs != null) {
            for (String doc : docs) {
                if (doc != null && !doc.isEmpty()) {
                    if (sysMsg.length() > 0) {
                        sysMsg.append("\n\n");
                    }
                    sysMsg.append("[参考文档]\n").append(Utils.truncate(doc, maxPerRefChars));
                }
            }
        }

        Map<String, Object> sysMsgForJson = new HashMap<>();
        sysMsgForJson.put("role", "system");
        sysMsgForJson.put("content", sysMsg.toString());
        return sysMsgForJson;
    }

    Map<String, Object> buildUserMessage(String message, Attachment[] attachments) {
        List<Object> parts = null;
        if (attachments != null && attachments.length > 0) {
            parts = new ArrayList<>();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", message);
            parts.add(textPart);

            for (Attachment attach : attachments) {
                if (attach instanceof ImageAttachment) {
                    parts.add(getImagePart((ImageAttachment) attach, imageDetail));
                }
            }
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", parts != null ? parts : message);
        return userMsg;
    }

    Map<String, Object> buildUserMessageForMaxToolIterations() {
        return buildUserMessage("已达工具调用次数上限，停止继续执行工具，无论数据是否充分，请给出最后结论并提醒工具调用次数已达上限。", null);
    }

    private Map<String, Object> getImagePart(ImageAttachment image, ImageDetail imageDetail) {
        Map<String, Object> imageUrl = new HashMap<>();
        String mimeType = image.mimeType != null ? image.mimeType : "image/jpeg";
        imageUrl.put("url", "data:" + mimeType + ";base64," + image.base64);
        if (imageDetail != null) {
            imageUrl.put("detail", imageDetail.name());
        }

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);
        return imagePart;
    }

    /**
     * 兼容 OpenAI 兼容接口中 content 为字符串或数组（多模态 delta：[{type:text,text:...}]）的两种返回格式。
     */
    String extractText(Object content) {
        if (content == null) return null;
        if (content instanceof String) return (String) content;
        if (content instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object item : (List<?>) content) {
                if (item instanceof Map) {
                    Object text = ((Map<?, ?>) item).get("text");
                    if (text instanceof String) {
                        sb.append((String) text);
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return String.valueOf(content);
    }
}
