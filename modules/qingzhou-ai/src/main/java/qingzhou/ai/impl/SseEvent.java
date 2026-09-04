package qingzhou.ai.impl;

import java.util.HashMap;
import java.util.Map;

class SseEvent {
    // 枚举名即 SSE 协议事件名，修改需同步前端
    // 段（思考 / 正文）的结束不单独发事件，由下一个段开始事件或 RUN_FINISHED / RUN_ERROR 推导
    enum Type {
        RUN_STARTED, STATUS, RUN_FINISHED, RUN_ERROR,
        REASONING_START, REASONING_CONTENT, REASONING_PAUSE,
        TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT,
        TOOL_CALL,
        USAGE
    }

    final Type type;
    final Map<String, Object> data = new HashMap<>();

    private SseEvent(Type type) {
        this.type = type;
    }

    static SseEvent of(Type type) {
        return new SseEvent(type);
    }

    SseEvent message(String message) {
        this.data.put("message", message);
        return this;
    }

    /** 阶段状态标识：matching / working 等，配合 STATUS 事件使用 */
    SseEvent stage(String stage) {
        this.data.put("stage", stage);
        return this;
    }

    /** 错误分类编码：MODEL_TIMEOUT / MODEL_ERROR 等，配合 RUN_ERROR 事件使用 */
    SseEvent code(String code) {
        this.data.put("code", code);
        return this;
    }

    SseEvent content(String content) {
        this.data.put("content", content);
        return this;
    }

    SseEvent toolName(String value) {
        this.data.put("toolName", value);
        return this;
    }

    /** 本轮请求的 token 用量，工具调用多轮时前端累加 */
    SseEvent usage(int promptTokens, int completionTokens, int totalTokens) {
        this.data.put("promptTokens", promptTokens);
        this.data.put("completionTokens", completionTokens);
        this.data.put("totalTokens", totalTokens);
        return this;
    }
}
