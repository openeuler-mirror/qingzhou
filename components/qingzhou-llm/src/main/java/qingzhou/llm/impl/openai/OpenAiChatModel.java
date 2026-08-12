package qingzhou.llm.impl.openai;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Tool;

class OpenAiChatModel implements ChatModel {
    private final OpenAiChatModelBuilder builder;
    private final HttpClient httpClient;
    private final Json json;

    private final Map<String, Tool> tools = new HashMap<>();

    OpenAiChatModel(OpenAiChatModelBuilder builder, HttpClient httpClient, Json json) {
        this.builder = builder;
        this.httpClient = httpClient;
        this.json = json;

        initTools();
    }

    private void initTools() {
        if (builder.tools != null) {
            builder.tools.forEach(tool -> tools.put(tool.name(), tool));
        }
        if (builder.skills != null) {
            builder.skills.forEach(skill -> {
                Collection<Tool> skillTools = skill.tools();
                if (skillTools != null) {
                    skillTools.forEach(tool -> tools.put(tool.name(), tool));
                }
            });
        }
    }

    @Override
    public void chat(String message, Listener listener, Attachment... attachment) {
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(OpenAiDialect.buildSystemMessage(builder.systemPrompt, builder.skills, builder.docs));
            messages.add(OpenAiDialect.buildUserMessage(message, attachment));

            List<Object> toolDefs = OpenAiDialect.buildToolDefinition(tools.values());

            listener.onBegin();
            String response = doChat(messages, toolDefs);

            messages.add(OpenAiDialect.buildAssistantMessage(response, null));

            listener.onComplete();
        } catch (Throwable t) {
            listener.onError(t);
        }
    }

    private String doChat(List<Object> messages, List<Object> toolDefs) throws Exception {
        // 构造请求参数
        String llmReq = json.toJson(OpenAiDialect.buildLlmRequest(builder.model, messages, toolDefs));
        Request request = httpClient.newRequest(builder.baseUrl);
        request.header("Content-Type", "application/json");
        request.header("Authorization", "Bearer " + builder.apiKey);
        request.header("Accept", "text/event-stream");
        request.body(llmReq.getBytes(StandardCharsets.UTF_8));

        // 发送请求
        Response response = httpClient.send(request);
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }
}
