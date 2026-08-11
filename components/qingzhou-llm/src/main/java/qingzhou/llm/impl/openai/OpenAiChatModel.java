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
import qingzhou.llm.impl.ImageAttachment;

class OpenAiChatModel implements ChatModel {
    private final OpenAiChatModelBuilder builder;
    private final ConnectionManager connectionManager;
    private final Json json;

    OpenAiChatModel(OpenAiChatModelBuilder builder, ConnectionManager connectionManager, Json json) {
        this.builder = builder;
        this.connectionManager = connectionManager;
        this.json = json;
    }

    @Override
    public void chat(String message, Listener listener, Attachment... attachment) {
        CHAT_EXECUTOR.submit(() -> {
            try {
                List<Object> messages = new ArrayList<>();
                messages.add(buildSystemMessage());
                messages.add(buildUserMessage(message, attachment));

                List<Object> toolDefs = buildToolDefinitions();

                listener.onBegin();

                for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                    StreamResult result = streamChat(messages, toolDefs, listener);

                    if (result.toolCalls == null || result.toolCalls.isEmpty()) {
                        break;
                    }

                    messages.add(result.toAssistantMessage());

                    listener.onReasoningPause();
                    for (Map<String, Object> toolCall : result.toolCalls) {
                        executeToolCall(toolCall, messages, listener);
                    }
                    listener.onReasoningResume();
                }

                listener.onComplete();
            } catch (Throwable t) {
                listener.onError(t);
            }
        });
    }


    private static final int MAX_TOOL_ITERATIONS = 20;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final ExecutorService CHAT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "qingzhou-llm");
        t.setDaemon(true);
        return t;
    });


    private Map<String, Object> buildSystemMessage() {
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

    private Map<String, Object> buildUserMessage(String message, Attachment[] attachments) {
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

    private List<Object> buildToolDefinitions() {
        List<Tool> allTools = new ArrayList<>();
        if (builder.tools != null) allTools.addAll(builder.tools);
        if (builder.skills != null) {
            for (Skill skill : builder.skills) {
                Collection<Tool> skillTools = skill.tools();
                if (skillTools != null) allTools.addAll(skillTools);
            }
        }

        if (allTools.isEmpty()) return Collections.emptyList();

        List<Object> toolDefs = new ArrayList<>();
        for (Tool tool : allTools) {
            Map<String, Object> toolDef = new HashMap<>();
            toolDef.put("type", "function");

            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");

            Parameter[] params = tool.parameters();
            if (params != null && params.length > 0) {
                Map<String, Object> properties = new HashMap<>();
                List<String> required = new ArrayList<>();
                for (Parameter param : params) {
                    Map<String, Object> prop = new HashMap<>();
                    prop.put("type", "string");
                    prop.put("description", param.description());
                    properties.put(param.name(), prop);
                    if (param.required()) {
                        required.add(param.name());
                    }
                }
                parameters.put("properties", properties);
                if (!required.isEmpty()) {
                    parameters.put("required", required);
                }
            } else {
                parameters.put("properties", new HashMap<>());
            }

            function.put("parameters", parameters);
            toolDef.put("function", function);
            toolDefs.add(toolDef);
        }
        return toolDefs;
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

    @SuppressWarnings("unchecked")
    private void executeToolCall(Map<String, Object> toolCall, List<Object> messages, Listener listener) {
        Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
        String toolName = (String) function.get("name");
        String argumentsStr = (String) function.get("arguments");

        Map<String, Object> args = new HashMap<>();
        if (argumentsStr != null && !argumentsStr.isEmpty()) {
            try {
                args = json.fromJson(argumentsStr, Map.class);
            } catch (Exception ignored) {
                args = new HashMap<>();
            }
        }

        Tool tool = findTool(toolName);
        String result;
        if (tool != null) {
            try {
                result = tool.invoke(args);
            } catch (Throwable t) {
                result = "Error: " + t.getMessage();
            }
        } else {
            result = "Tool not found: " + toolName;
        }

        listener.onToolCall(toolName, args, result);

        Map<String, Object> toolResultMsg = new HashMap<>();
        toolResultMsg.put("role", "tool");
        toolResultMsg.put("tool_call_id", toolCall.get("id"));
        toolResultMsg.put("content", result);
        messages.add(toolResultMsg);
    }

    private Tool findTool(String name) {
        if (builder.tools != null) {
            for (Tool tool : builder.tools) {
                if (tool.name().equals(name)) return tool;
            }
        }
        if (builder.skills != null) {
            for (Skill skill : builder.skills) {
                Collection<Tool> skillTools = skill.tools();
                if (skillTools != null) {
                    for (Tool tool : skillTools) {
                        if (tool.name().equals(name)) return tool;
                    }
                }
            }
        }
        return null;
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
