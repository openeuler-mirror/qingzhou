package qingzhou.llm.impl.openai;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Tool;

class OpenAiChatModel implements ChatModel {
    private static final int MAX_TOOL_ITERATIONS = 20;
    private static final int MAX_RETRIES = 3;

    private final OpenAiChatModelBuilder builder;
    private final HttpClient httpClient;
    private final Json json;
    private final Map<String, Tool> tools = new HashMap<>();

    OpenAiChatModel(OpenAiChatModelBuilder builder, HttpClient httpClient, Json json) {
        this.builder = builder;
        this.httpClient = httpClient;
        this.json = json;

        if (builder.tools != null) {
            builder.tools.forEach(tool -> tools.put(tool.name(), tool));
        }
        if (builder.skills != null) {
            builder.skills.forEach(skill -> {
                if (skill.tools() != null) {
                    skill.tools().forEach(tool -> tools.put(tool.name(), tool));
                }
            });
        }
    }

    @Override
    public void chat(String message, Listener chatListener, Attachment... attachment) {
        try {
            Map<String, Object> systemMessage = OpenAiDialect.buildSystemMessage(builder.systemPrompt, builder.skills, builder.docs);
            Map<String, Object> userMessage = OpenAiDialect.buildUserMessage(message, attachment);
            List<Object> toolDefinitions = OpenAiDialect.buildToolDefinitions(tools.values());

            List<Object> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMessage);
            chatListener.onBegin();
            doChat(messages, toolDefinitions, chatListener, 0);
        } catch (Throwable t) {
            chatListener.onError(t.getMessage());
        }
    }

    private void doChat(List<Object> messages, List<Object> toolDefs, Listener chatListener, int toolIteration) throws Exception {
        if (toolIteration >= MAX_TOOL_ITERATIONS) {
            chatListener.onComplete();
            return;
        }

        Map<String, Object> llmRequest = OpenAiDialect.buildLlmRequest(builder.model, messages, toolDefs);
        Request request = httpClient.newRequest(builder.baseUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + builder.apiKey)
                .header("Accept", "text/event-stream");
        request.body(json.toJson(llmRequest).getBytes(StandardCharsets.UTF_8));

        HttpListener httpListener = new HttpListener(messages, toolDefs, chatListener, toolIteration);
        for (int attempt = 0; ; attempt++) {
            Response response = httpClient.send(request, httpListener);
            if (response.getStatus() == 200) return;

            response.cancel();
            if (response.getStatus() == 429 && attempt < MAX_RETRIES) {
                Thread.sleep(1000L << attempt); // 429 限流：指数退避后重试
                continue;
            }
            chatListener.onError("API error " + response.getStatus() + ": " + new String(response.getBody(), StandardCharsets.UTF_8));
            return;
        }
    }

    private class HttpListener implements ResponseListener {
        private final List<Object> messages;
        private final List<Object> toolDefs;
        private final Listener chatListener;
        private final int toolIteration;

        private final StringBuilder content = new StringBuilder();
        private final Map<Integer, ToolCall> toolCalls = new TreeMap<>();

        HttpListener(List<Object> messages, List<Object> toolDefs, Listener chatListener, int toolIteration) {
            this.messages = messages;
            this.toolDefs = toolDefs;
            this.chatListener = chatListener;
            this.toolIteration = toolIteration;
        }

        @Override
        public void onBody(String line) {
            if (line == null || !line.startsWith("data:")) return;

            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) return;

            try {
                Map<String, Object> chunk = json.fromJson(data, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                if (choices == null || choices.isEmpty()) return;

                Map<String, Object> choice = choices.get(0);
                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                if (delta == null) return;

                String reasoning = (String) delta.get("reasoning_content");
                if (reasoning != null && !reasoning.isEmpty()) {
                    chatListener.onReasoning(reasoning);
                }

                String text = (String) delta.get("content");
                if (text != null && !text.isEmpty()) {
                    content.append(text);
                    chatListener.onMessage(text);
                }

                List<Map<String, Object>> deltaToolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                if (deltaToolCalls != null) {
                    for (Map<String, Object> dtc : deltaToolCalls) {
                        int index = ((Number) dtc.get("index")).intValue();
                        ToolCall toolCall = toolCalls.computeIfAbsent(index, k -> new ToolCall());
                        if (dtc.get("id") != null) {
                            toolCall.id = String.valueOf(dtc.get("id"));
                        }
                        Map<String, Object> dfn = (Map<String, Object>) dtc.get("function");
                        if (dfn != null) {
                            if (dfn.get("name") != null) {
                                toolCall.name = String.valueOf(dfn.get("name"));
                            }
                            if (dfn.get("arguments") != null) {
                                String args = toolCall.arguments != null ? toolCall.arguments : "";
                                toolCall.arguments = args + dfn.get("arguments");
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // 单行解析失败不影响流式输出
            }
        }

        @Override
        public void onComplete() {
            try {
                if (!toolCalls.isEmpty()) {
                    messages.add(OpenAiDialect.buildAssistantMessage(content.toString(), new ArrayList<>(toolCalls.values())));

                    chatListener.onReasoningPause();
                    for (ToolCall toolCall : toolCalls.values()) {
                        chatListener.onToolCall(toolCall.name);

                        String result;
                        Tool tool = tools.get(toolCall.name);
                        if (tool != null) {
                            Map<String, Object> args = null;
                            if (toolCall.arguments != null && !toolCall.arguments.isEmpty()) {
                                try {
                                    args = json.fromJson(toolCall.arguments, Map.class);
                                } catch (Exception ignored) {
                                }
                            }
                            try {
                                result = tool.invoke(args);
                            } catch (Throwable t) {
                                result = "Error: " + t.getMessage();
                            }
                        } else {
                            result = "Tool not found: " + toolCall.name;
                        }

                        messages.add(OpenAiDialect.buildToolMessage(toolCall.id, result));
                    }
                    chatListener.onReasoningResume();

                    doChat(messages, toolDefs, chatListener, toolIteration + 1);
                } else {
                    chatListener.onComplete();
                }
            } catch (Throwable t) {
                chatListener.onError(t.getMessage());
            }
        }

        @Override
        public void onError(Throwable t) {
            chatListener.onError(t.getMessage());
        }
    }

    static class ToolCall {
        String id;
        String name;
        String arguments;
    }
}
