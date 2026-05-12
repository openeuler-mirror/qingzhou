package qingzhou.ai.impl;

import java.util.HashMap;
import java.util.Map;

class SseResult {

    final String type;
    final Map<String, String> data = new HashMap<>();

    SseResult(String type) {
        this.type = type;
    }

    static SseResult type(String type) {
        return new SseResult(type);
    }

    SseResult message(String message) {
        this.data.put("message", message);
        return this;
    }

    SseResult messageId(String messageId) {
        this.data.put("messageId", messageId);
        return this;
    }

    SseResult content(String content) {
        this.data.put("content", content);
        return this;
    }
}
