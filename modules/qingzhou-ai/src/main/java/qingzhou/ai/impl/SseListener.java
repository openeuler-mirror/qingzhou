package qingzhou.ai.impl;

import java.util.UUID;

import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.Listener;
import qingzhou.logger.Logger;

public class SseListener implements Listener {
    private final HttpResponse httpResponse;
    private final Logger logger;
    private final Json json;
    private final String messageId;

    private boolean isReasoning = false;
    private boolean isMessage = false;

    public SseListener(HttpResponse httpResponse, Logger logger, Json json) {
        this.httpResponse = httpResponse;
        this.logger = logger;
        this.json = json;
        this.messageId = UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void onBegin() {
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.RUN_STARTED)));
    }

    @Override
    public void onReasoning(String content) {
        if (!isReasoning) {
            isReasoning = true;
            if (isMessage) {
                httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_END).messageId(messageId)));
            }
            isMessage = false;
            httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.REASONING_START)));
        }
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.REASONING_CONTENT).content(content)));
    }

    @Override
    public void onReasoningPause() {
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.REASONING_PAUSE)));
    }

    @Override
    public void onReasoningResume() {
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.REASONING_RESUME)));
    }

    @Override
    public void onToolCall(String toolName) {
        try {
            httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.TOOL_CALL).toolName(toolName)));
        } catch (Exception e) {
            logger.error("failed to serialize tool call: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(String content) {
        if (!isMessage) {
            isMessage = true;
            if (isReasoning) {
                httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.REASONING_END)));
            }
            isReasoning = false;
            httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_START).messageId(messageId)));
        }
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_CONTENT).messageId(messageId).content(content)));
    }

    @Override
    public void onUsage(int promptTokens, int completionTokens, int totalTokens) {
        logger.info("LLM usage: prompt=" + promptTokens + ", completion=" + completionTokens + ", total=" + totalTokens);
    }

    @Override
    public void onError(String error) {
        logger.error(error);
        try {
            httpResponse.sendFinish(toSseText(SseEvent.of(SseEvent.Type.RUN_ERROR).message(error)));
        } catch (Exception e) {
            // 客户端已断开连接，无法发送错误信息，忽略
        }
    }

    @Override
    public void onComplete() {
        httpResponse.send(toSseText(SseEvent.of(SseEvent.Type.TEXT_MESSAGE_END).messageId(messageId)));
        httpResponse.sendFinish(toSseText(SseEvent.of(SseEvent.Type.RUN_FINISHED)));
    }

    private String toSseText(SseEvent event) {
        String toJson;
        try {
            toJson = json.toJson(event.data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            toJson = e.getMessage();
        }
        return String.format("event: %s\ndata: %s\n\n", event.type, toJson);
    }
}
