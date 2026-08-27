package qingzhou.ai.impl;

import java.util.HashMap;
import java.util.Map;

class SseEvent {
    // 枚举名即 SSE 协议事件名，修改需同步前端
    enum Type {
        RUN_STARTED, RUN_FINISHED, RUN_ERROR,
        REASONING_START, REASONING_CONTENT, REASONING_PAUSE, REASONING_RESUME, REASONING_END,
        TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT, TEXT_MESSAGE_END,
        TOOL_CALL
    }

    final Type type;
    final Map<String, String> data = new HashMap<>();

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

    SseEvent messageId(String messageId) {
        this.data.put("messageId", messageId);
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
}
