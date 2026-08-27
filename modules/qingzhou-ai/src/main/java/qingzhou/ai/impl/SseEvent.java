package qingzhou.ai.impl;

import java.util.HashMap;
import java.util.Map;

class SseEvent {

    final String type;
    final Map<String, String> data = new HashMap<>();

    SseEvent(String type) {
        this.type = type;
    }

    static SseEvent of(String type) {
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
