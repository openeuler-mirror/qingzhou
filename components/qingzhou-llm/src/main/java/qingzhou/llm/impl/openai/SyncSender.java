package qingzhou.llm.impl.openai;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;
import qingzhou.llm.impl.ToolCallInfo;
import qingzhou.llm.impl.Utils;

class SyncSender {
    private final OpenAiChatModelBuilder builder;
    private final HttpClient httpClient;
    private final Json json;

    SyncSender(OpenAiChatModelBuilder builder, HttpClient httpClient, Json json) {
        this.builder = builder;
        this.httpClient = httpClient;
        this.json = json;
    }

    String chat(Map<String, Tool> baseTools, String message, Attachment... attachment) {
        try {
            List<Skill> activeSkills = Utils.getActiveSkills(builder.skills, message, () -> new OpenAiChatModelBuilder(builder.baseUrl, builder.apiKey, builder.model, httpClient, json));
            Map<String, Tool> activeTools = Utils.getActiveTools(activeSkills, baseTools);

            List<Object> messages = new ArrayList<>();
            messages.add(builder.buildSystemMessage(activeSkills));
            messages.add(builder.buildUserMessage(message, attachment));
            List<Object> toolDefs = builder.buildToolDefinitions(activeTools.values());

            for (int i = 0; i < builder.maxToolIterations; i++) {
                Response response = sendSync(messages, toolDefs, 0);
                Map<String, Object> msg = getResponseMessage(response);
                if (msg == null) return "";

                List<ToolCallInfo> toolCalls = parseToolCalls((List<Map<String, Object>>) msg.get("tool_calls"));
                if (toolCalls.isEmpty()) {
                    String content = builder.extractText(msg.get("content"));
                    return content != null ? content : "";
                }
                messages.add(msg);
                for (ToolCallInfo toolCall : toolCalls) {
                    messages.add(builder.buildToolMessage(toolCall.id, Utils.invokeTool(toolCall, activeTools, json)));
                }
            }
            Utils.println("Tool iterations have reached the limit: " + builder.maxToolIterations);
            messages.add(builder.buildUserMessageForMaxToolIterations());
            Response response = sendSync(messages, null, 0); // 工具调用到最大轮次后，也需要无工具再请求一次，强制要求给出最后结论
            Map<String, Object> msg = getResponseMessage(response);
            if (msg == null) return "";
            String content = builder.extractText(msg.get("content"));
            return content != null ? content : "";
        } catch (Throwable t) {
            Utils.println("Chat failed: " + Utils.errorMessage(t));
            return null;
        }
    }

    private Response sendSync(List<Object> messages, List<Object> toolDefs, int attempt) throws Exception {
        Response response;
        try {
            Request request = Utils.newLlmRequest(builder.buildLlmRequest(messages, toolDefs, false), false, builder, httpClient, json);
            response = httpClient.send(request);
        } catch (Exception e) {
            if (attempt < builder.maxRetries) {
                Utils.sleepBackoff(attempt);
                return sendSync(messages, toolDefs, attempt + 1);
            }
            throw e;
        }
        int status = response.getStatus();
        if (status != 200) {
            if ((status == 429 || status >= 500) && attempt < builder.maxRetries) {
                Utils.sleepBackoff(attempt);
                return sendSync(messages, toolDefs, attempt + 1);
            }
            throw new IllegalStateException("API error " + status + ": " + new String(response.getBody(), StandardCharsets.UTF_8));
        }
        return response;
    }

    private List<ToolCallInfo> parseToolCalls(List<Map<String, Object>> toolCalls) {
        List<ToolCallInfo> result = new ArrayList<>();
        if (toolCalls == null) return result;

        for (Map<String, Object> tc : toolCalls) {
            Map<String, Object> fn = (Map<String, Object>) tc.get("function");
            if (fn == null) continue;
            ToolCallInfo call = new ToolCallInfo();
            Object id = tc.get("id");
            call.id = id != null ? String.valueOf(id) : "";
            Object name = fn.get("name");
            call.name = name != null ? String.valueOf(name) : "";
            Object arguments = fn.get("arguments");
            call.arguments = arguments != null ? String.valueOf(arguments) : null;
            result.add(call);
        }
        return result;
    }

    private Map<String, Object> getResponseMessage(Response response) throws Exception {
        Map<String, Object> data = json.fromJson(new String(response.getBody(), StandardCharsets.UTF_8), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        return (Map<String, Object>) choices.get(0).get("message");
    }
}
