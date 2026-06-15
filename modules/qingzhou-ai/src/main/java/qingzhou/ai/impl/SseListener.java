package qingzhou.ai.impl;

import java.util.Map;
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
        httpResponse.send(resultToString(SseResult.type("RUN_STARTED")));
    }

    @Override
    public void onReasoning(String content) {
        if (!isReasoning) {
            isReasoning = true;
            if (isMessage) {
                httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
            }
            isMessage = false;
            httpResponse.send(resultToString(SseResult.type("REASONING_START")));
        }
        httpResponse.send(resultToString(SseResult.type("REASONING_CONTENT").content(content)));
    }

    @Override
    public void onReasoningPause() {
        httpResponse.send(resultToString(SseResult.type("REASONING_PAUSE")));
    }

    @Override
    public void onReasoningResume() {
        httpResponse.send(resultToString(SseResult.type("REASONING_RESUME")));
    }

    @Override
    public void onToolCall(String toolName, Map<String, Object> args, Object result) {
        try {
            httpResponse.send(resultToString(
                    SseResult.type("TOOL_CALL")
                            .content(json.toJson(args))
                            .message(json.toJson(result))
                            .toolName(toolName)
            ));
        } catch (Exception e) {
            logger.error("failed to serialize tool call: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(String content) {
        if (!isMessage) {
            isMessage = true;
            if (isReasoning) {
                httpResponse.send(resultToString(SseResult.type("REASONING_END")));
            }
            isReasoning = false;
            httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_START").messageId(messageId)));
        }
        httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_CONTENT").messageId(messageId).content(content)));
    }

    @Override
    public void onError(Throwable t) {
        String errMsg = t.getMessage();
        logger.error(errMsg);
        try {
            httpResponse.sendFinish(resultToString(SseResult.type("RUN_ERROR").message(errMsg)));
        } catch (Exception e) {
            // 客户端已断开连接，无法发送错误信息，忽略
        }
    }

    @Override
    public void onComplete() {
        httpResponse.send(resultToString(SseResult.type("TEXT_MESSAGE_END").messageId(messageId)));
        httpResponse.sendFinish(resultToString(SseResult.type("RUN_FINISHED")));
    }

    private String resultToString(SseResult result) {
        return Chat.resultToString(result, json);
    }
}
