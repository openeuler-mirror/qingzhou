package qingzhou.llm.impl.openai;

import java.nio.charset.StandardCharsets;
import java.util.*;

import qingzhou.http.client.HttpClient;
import qingzhou.http.client.Request;
import qingzhou.http.client.Response;
import qingzhou.http.client.ResponseListener;
import qingzhou.json.Json;
import qingzhou.llm.*;
import qingzhou.llm.impl.ToolCallInfo;
import qingzhou.llm.impl.Utils;

class OpenAiChatModel implements ChatModel {
    private final OpenAiChatModelBuilder builder;
    private final HttpClient httpClient;
    private final Json json;
    private final Map<String, Tool> baseTools = new HashMap<>();

    private final SyncSender syncSender;

    OpenAiChatModel(OpenAiChatModelBuilder builder, HttpClient httpClient, Json json) {
        this.builder = builder;
        this.httpClient = httpClient;
        this.json = json;

        if (builder.tools != null) {
            builder.tools.forEach(tool -> baseTools.put(tool.name(), tool));
        }

        syncSender = new SyncSender(builder, httpClient, json);
    }

    @Override
    public String chat(String message, Attachment... attachment) {
        return syncSender.chat(baseTools, message, attachment);
    }

    @Override
    public void chat(String message, Listener chatListener, Attachment... attachment) {
        try {
            if (builder.skills != null && !builder.skills.isEmpty()) {
                // 技能匹配是同步的 LLM 调用且期间无其它事件，先告知客户端当前阶段，避免误判卡死
                chatListener.onSkillMatching();
            }
            List<Skill> activeSkills = Utils.getActiveSkills(builder.skills, message, () -> new OpenAiChatModelBuilder(builder.baseUrl, builder.apiKey, builder.model, httpClient, json));
            Map<String, Tool> activeTools = Utils.getActiveTools(activeSkills, baseTools);

            Map<String, Object> systemMessage = builder.buildSystemMessage(activeSkills);
            Map<String, Object> userMessage = builder.buildUserMessage(message, attachment);
            List<Object> toolDefinitions = builder.buildToolDefinitions(activeTools.values());

            List<Object> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(userMessage);
            // RUN_STARTED 已由 HTTP 层在受理请求时发出（见 AiChat），LLM 层不再负责会话生命周期
            doChat(messages, toolDefinitions, chatListener, activeTools, 0);
        } catch (Throwable t) {
            chatListener.onError(Utils.errorMessage(t));
        }
    }

    /**
     * 一轮模型对话：新建流式监听器并发送请求（含 429/5xx/网络异常重试）。
     * 请求返回 200 后本方法立即返回，流式读取与后续回调（onBody/onComplete/onError）在后台线程进行。
     */
    private void doChat(List<Object> messages, List<Object> toolDefs, Listener chatListener, Map<String, Tool> tools, int toolIteration) {
        if (toolIteration >= builder.maxToolIterations) { // 达到工具调用上限：明确告知调用方，避免静默终止
            Utils.println("Tool iterations have reached the limit: " + toolIteration);
            messages.add(builder.buildUserMessageForMaxToolIterations());
            toolDefs = null; // 工具调用到最大轮次后，也需要无工具再请求一次，强制要求给出最后结论
        }
        HttpListener httpListener = new HttpListener(messages, toolDefs, chatListener, tools, toolIteration);
        sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, 0);
    }

    /**
     * 发送请求并对瞬时故障（HTTP 429/5xx、连接失败/超时等网络异常）做指数退避重试。
     * 200 时立即返回，流式读取交给 httpListener 在后台线程进行。
     */
    private void sendWithRetry(List<Object> messages, List<Object> toolDefs, Listener chatListener, int toolIteration,
                               HttpListener httpListener, int attempt) {
        try {
            Request request = Utils.newLlmRequest(builder.buildLlmRequest(messages, toolDefs, true), true, builder, httpClient, json);
            Response response = httpClient.send(request, httpListener);
            if (response.getStatus() == 200) return;

            response.cancel();
            if ((response.getStatus() == 429 || response.getStatus() >= 500) && attempt < builder.maxRetries) {
                Utils.sleepBackoff(attempt);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, attempt + 1);
                return;
            }
            chatListener.onError("API error " + response.getStatus() + ": " + new String(response.getBody(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 网络异常（连接失败/超时等）同样属于瞬时故障，参与指数退避重试
            if (attempt < builder.maxRetries) {
                Utils.sleepBackoff(attempt);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, httpListener, attempt + 1);
                return;
            }
            chatListener.onError(Utils.errorMessage(e));
        }
    }

    private class HttpListener implements ResponseListener {
        private final List<Object> messages;
        private final List<Object> toolDefs;
        private final Listener chatListener;
        private final Map<String, Tool> tools;
        private final int toolIteration;

        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoningContent = new StringBuilder();
        private final Map<Integer, ToolCallInfo> toolCalls = new TreeMap<>();
        private boolean streamRetried; // 流式读取中断后是否已重发过（仅允许一次，避免重复输出）
        private String finishReason; // 最后一个 chunk 的 finish_reason（stop / length / tool_calls ...）
        private Map<String, Object> usage; // 流式末尾 usage chunk（依赖 stream_options.include_usage）

        HttpListener(List<Object> messages, List<Object> toolDefs, Listener chatListener, Map<String, Tool> tools, int toolIteration) {
            this.messages = messages;
            this.toolDefs = toolDefs;
            this.chatListener = chatListener;
            this.tools = tools;
            this.toolIteration = toolIteration;
        }

        @Override
        public void onBody(String line) {
            if (line == null || !line.startsWith("data:")) return;

            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) return;

            try {
                Map<String, Object> chunk = json.fromJson(data, Map.class);

                // 流式末尾的 usage chunk：choices 为空、携带 usage（依赖 stream_options.include_usage）
                Map<String, Object> chunkUsage = (Map<String, Object>) chunk.get("usage");
                if (chunkUsage != null && !chunkUsage.isEmpty()) {
                    usage = chunkUsage;
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                if (choices == null || choices.isEmpty()) return;

                Map<String, Object> choice = choices.get(0);

                Object finishReasonObj = choice.get("finish_reason");
                if (finishReasonObj != null) {
                    finishReason = String.valueOf(finishReasonObj);
                }

                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                if (delta == null) return;

                String reasoning = builder.extractReasoning(delta);
                if (!reasoning.isEmpty()) {
                    chatListener.onReasoning(reasoning);
                    reasoningContent.append(reasoning);
                }

                // 兼容 content 为字符串或数组（多模态 delta：[{type:text,text:...}]）的返回格式
                String text = builder.extractText(delta.get("content"));
                if (text != null && !text.isEmpty()) {
                    chatListener.onMessage(text);
                    content.append(text);
                }

                List<Map<String, Object>> deltaToolCalls = (List<Map<String, Object>>) delta.get("tool_calls");
                if (deltaToolCalls != null) {
                    for (Map<String, Object> dtc : deltaToolCalls) {
                        Object indexObj = dtc.get("index");
                        if (indexObj == null) continue; // 缺少 index 的增量无法正确归位，跳过
                        int index = ((Number) indexObj).intValue();
                        ToolCallInfo toolCallInfo = toolCalls.computeIfAbsent(index, k -> new ToolCallInfo());
                        if (dtc.get("id") != null) {
                            toolCallInfo.id = String.valueOf(dtc.get("id"));
                        }
                        Map<String, Object> dfn = (Map<String, Object>) dtc.get("function");
                        if (dfn != null) {
                            if (dfn.get("name") != null) {
                                toolCallInfo.name = String.valueOf(dfn.get("name"));
                            }
                            if (dfn.get("arguments") != null) {
                                String args = toolCallInfo.arguments != null ? toolCallInfo.arguments : "";
                                toolCallInfo.arguments = args + dfn.get("arguments");
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
                if (usage != null) {
                    chatListener.onUsage(
                            ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue(),
                            ((Number) usage.getOrDefault("completion_tokens", 0)).intValue(),
                            ((Number) usage.getOrDefault("total_tokens", 0)).intValue());
                }

                if (!toolCalls.isEmpty()) {
                    // 前次的思考内容在后面轮次请求时也会提交，（deepseek 强制要求存在tool参数时，将前面的思维链内容带上，其他国内厂商的最新模型也有这个要求）
                    messages.add(builder.buildAssistantMessage(content.toString(), reasoningContent.toString(), toolCalls.values()));

                    chatListener.onReasoningPause();
                    for (ToolCallInfo toolCallInfo : toolCalls.values()) {
                        chatListener.onToolCall(toolCallInfo.name);
                        messages.add(builder.buildToolMessage(toolCallInfo.id, Utils.invokeTool(toolCallInfo, tools, json)));
                    }

                    doChat(messages, toolDefs, chatListener, tools, toolIteration + 1);
                } else {
                    if ("length".equals(finishReason)) {
                        // 输出被 max_tokens 等长度上限截断，明确告知调用方
                        chatListener.onMessage("（输出已达长度上限，内容被截断）");
                    }
                    chatListener.onComplete();
                }
            } catch (Throwable t) {
                chatListener.onError(Utils.errorMessage(t));
            }
        }

        @Override
        public void onError(Throwable t) {
            // 流式读取中断（如网络抖动）：若尚未输出任何内容（无正文、无工具调用），静默重发一次
            if (!streamRetried && content.length() == 0 && toolCalls.isEmpty()) {
                streamRetried = true;
                Utils.sleepBackoff(0);
                sendWithRetry(messages, toolDefs, chatListener, toolIteration, this, 0);
                return;
            }
            chatListener.onError(Utils.errorMessage(t));
        }
    }
}
