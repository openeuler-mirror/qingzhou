package qingzhou.llm.impl.openai;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.llm.impl.ConnectionManager;

class OpenAiChatModel implements ChatModel {
    private static final int MAX_TOOL_ITERATIONS = 20;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final ExecutorService CHAT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "qingzhou-llm");
        t.setDaemon(true);
        return t;
    });

    private final OpenAiChatModelBuilder builder;
    private final ConnectionManager connectionManager;
    private final Json json;
    private final MessageBuilder messageBuilder;
    private final ToolHandler toolHandler;

    OpenAiChatModel(OpenAiChatModelBuilder builder, ConnectionManager connectionManager, Json json) {
        this.builder = builder;
        this.connectionManager = connectionManager;
        this.json = json;
        this.messageBuilder = new MessageBuilder(builder);
        this.toolHandler = new ToolHandler(builder, json);
    }

    @Override
    public void chat(String message, Listener listener, Attachment... attachment) {
        CHAT_EXECUTOR.submit(() -> {
            try {
                List<Object> messages = new ArrayList<>();
                messages.add(messageBuilder.buildSystemMessage());
                messages.add(messageBuilder.buildUserMessage(message, attachment));

                List<Object> toolDefs = toolHandler.buildToolDefinitions();

                listener.onBegin();

                for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                    StreamResult result = streamChat(messages, toolDefs, listener);

                    if (result.toolCalls == null || result.toolCalls.isEmpty()) {
                        break;
                    }

                    messages.add(result.toAssistantMessage());

                    listener.onReasoningPause();
                    for (Map<String, Object> toolCall : result.toolCalls) {
                        toolHandler.executeToolCall(toolCall, messages, listener);
                    }
                    listener.onReasoningResume();
                }

                listener.onComplete();
            } catch (Throwable t) {
                listener.onError(t);
            }
        });
    }

    private StreamResult streamChat(List<Object> messages, List<Object> toolDefs,
                                    Listener listener) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", builder.model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        if (toolDefs != null && !toolDefs.isEmpty()) {
            requestBody.put("tools", toolDefs);
        }

        String jsonBody = json.toJson(requestBody);

        for (int attempt = 0; ; attempt++) {
            HttpURLConnection conn = connectionManager.getConnection();
            try {
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                int status = conn.getResponseCode();

                // 429 限流：指数退避 + 随机抖动后重试
                if (status == 429 && attempt < MAX_RETRIES) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << attempt);
                    backoffMs += (long) (Math.random() * backoffMs * 0.5);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted on 429", e);
                    }
                    continue;
                }

                if (status != 200) {
                    String errorBody = readAll(conn.getErrorStream());
                    throw new RuntimeException("API error " + status + ": " + errorBody);
                }

                StringBuilder contentBuilder = new StringBuilder();
                Map<Integer, Map<String, Object>> toolCallMap = new TreeMap<>();
                boolean hasToolCalls = false;

                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if (data.isEmpty() || data.equals("[DONE]")) continue;

                        Map<String, Object> chunk = json.fromJson(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices == null || choices.isEmpty()) continue;

                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                        if (delta == null) continue;

                        String reasoning = (String) delta.get("reasoning_content");
                        if (reasoning != null && !reasoning.isEmpty()) {
                            listener.onReasoning(reasoning);
                        }

                        String content = (String) delta.get("content");
                        if (content != null && !content.isEmpty()) {
                            contentBuilder.append(content);
                            listener.onMessage(content);
                        }

                        List<Map<String, Object>> deltaToolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                        if (deltaToolCalls != null) {
                            hasToolCalls = true;
                            for (Map<String, Object> dtc : deltaToolCalls) {
                                int index = ((Number) dtc.get("index")).intValue();
                                Map<String, Object> accumulated = toolCallMap.computeIfAbsent(index, k -> new HashMap<>());

                                if (dtc.containsKey("id")) {
                                    accumulated.put("id", dtc.get("id"));
                                }
                                if (dtc.containsKey("type")) {
                                    accumulated.put("type", dtc.get("type"));
                                }

                                Map<String, Object> function = (Map<String, Object>) accumulated.computeIfAbsent("function", k -> new HashMap<>());
                                Map<String, Object> deltaFunction = (Map<String, Object>) dtc.get("function");
                                if (deltaFunction != null) {
                                    if (deltaFunction.containsKey("name")) {
                                        function.put("name", deltaFunction.get("name"));
                                    }
                                    if (deltaFunction.containsKey("arguments")) {
                                        String args = (String) function.getOrDefault("arguments", "");
                                        args += deltaFunction.get("arguments");
                                        function.put("arguments", args);
                                    }
                                }
                            }
                        }

                        String finishReason = (String) choice.get("finish_reason");
                        if ("tool_calls".equals(finishReason)) {
                            hasToolCalls = true;
                        }
                    }
                }

                StreamResult result = new StreamResult();
                result.content = contentBuilder.toString();
                if (hasToolCalls) {
                    result.toolCalls = new ArrayList<>(toolCallMap.values());
                }
                return result;
            } finally {
                conn.disconnect();
            }
        }
    }

    private String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 8];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class StreamResult {
        String content;
        List<Map<String, Object>> toolCalls;

        Map<String, Object> toAssistantMessage() {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "assistant");
            if (content != null && !content.isEmpty()) {
                msg.put("content", content);
            }
            msg.put("tool_calls", toolCalls);
            return msg;
        }
    }
}
