package qingzhou.llm.impl.openai;

import java.net.URL;
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

        // 安全校验：API Key 不应通过明文 HTTP 传输（本机回环除外）
        validateBaseUrl(builder.baseUrl);

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
            chatListener.onError(errorMessage(t));
        }
    }

    /**
     * 一轮模型对话：新建流式监听器并发送请求（含 429/5xx/网络异常重试）。
     * 请求返回 200 后本方法立即返回，流式读取与后续回调（onBody/onComplete/onError）在后台线程进行。
     */
    private void doChat(List<Object> messages, List<Object> toolDefs, Listener chatListener, int toolIteration) {
        if (toolIteration >= MAX_TOOL_ITERATIONS) {
            // 达到工具调用上限：明确告知调用方，避免静默终止
            chatListener.onMessage("（已达工具调用次数上限，停止继续执行工具）");
            chatListener.onComplete();
            return;
        }

        HttpListener httpListener = new HttpListener(messages, toolDefs, chatListener, toolIteration);
        sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, 0);
    }

    /**
     * 发送请求并对瞬时故障（HTTP 429/5xx、连接失败/超时等网络异常）做指数退避重试。
     * 200 时立即返回，流式读取交给 httpListener 在后台线程进行。
     */
    private void sendWithRetry(List<Object> messages, List<Object> toolDefs, Listener chatListener, int toolIteration,
                               HttpListener httpListener, int attempt) {
        try {
            Map<String, Object> llmRequest = OpenAiDialect.buildLlmRequest(builder.model, messages, toolDefs);
            Request request = httpClient.newRequest(builder.baseUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + builder.apiKey)
                    .header("Accept", "text/event-stream");
            request.body(json.toJson(llmRequest).getBytes(StandardCharsets.UTF_8));

            Response response = httpClient.send(request, httpListener);
            if (response.getStatus() == 200) return;

            response.cancel();
            if ((response.getStatus() == 429 || response.getStatus() >= 500) && attempt < MAX_RETRIES) {
                sleepBackoff(attempt);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, attempt + 1);
                return;
            }
            chatListener.onError("API error " + response.getStatus() + ": " + new String(response.getBody(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 网络异常（连接失败/超时等）同样属于瞬时故障，参与指数退避重试
            if (attempt < MAX_RETRIES) {
                sleepBackoff(attempt);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, attempt + 1);
                return;
            }
            chatListener.onError(errorMessage(e));
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(1000L << attempt); // 指数退避：1s / 2s / 4s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String errorMessage(Throwable t) {
        if (t == null) return "unknown error";
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM baseUrl is missing");
        }
        try {
            URL url = new URL(baseUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Unsupported LLM baseUrl protocol: " + scheme);
            }
            if ("http".equalsIgnoreCase(scheme) && !isLoopback(host)) {
                throw new IllegalArgumentException("LLM baseUrl must use https (http is only allowed for loopback addresses like localhost)");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid LLM baseUrl: " + baseUrl, e);
        }
    }

    private boolean isLoopback(String host) {
        if (host == null) return false;
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private class HttpListener implements ResponseListener {
        private final List<Object> messages;
        private final List<Object> toolDefs;
        private final Listener chatListener;
        private final int toolIteration;

        private final StringBuilder content = new StringBuilder();
        private final Map<Integer, ToolCall> toolCalls = new TreeMap<>();
        private boolean streamRetried; // 流式读取中断后是否已重发过（仅允许一次，避免重复输出）

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

                // 兼容 content 为字符串或数组（多模态 delta：[{type:text,text:...}]）的返回格式
                String text = extractText(delta.get("content"));
                if (text != null && !text.isEmpty()) {
                    content.append(text);
                    chatListener.onMessage(text);
                }

                List<Map<String, Object>> deltaToolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                if (deltaToolCalls != null) {
                    for (Map<String, Object> dtc : deltaToolCalls) {
                        Object indexObj = dtc.get("index");
                        if (indexObj == null) continue; // 缺少 index 的增量无法正确归位，跳过
                        int index = ((Number) indexObj).intValue();
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
                                // 仅回传异常概要，避免把堆栈/内部路径等敏感信息暴露给模型
                                result = "Error: " + errorMessage(t);
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
                chatListener.onError(errorMessage(t));
            }
        }

        @Override
        public void onError(Throwable t) {
            // 流式读取中断（如网络抖动）：若尚未输出任何内容（无正文、无工具调用），静默重发一次
            if (!streamRetried && content.length() == 0 && toolCalls.isEmpty()) {
                streamRetried = true;
                sleepBackoff(0);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, this, 0);
                return;
            }
            chatListener.onError(errorMessage(t));
        }
    }

    /**
     * 兼容 OpenAI 兼容接口中 content 为字符串或数组（多模态 delta：[{type:text,text:...}]）的两种返回格式。
     */
    private String extractText(Object content) {
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

    static class ToolCall {
        String id;
        String name;
        String arguments;
    }
}
