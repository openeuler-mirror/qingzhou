package qingzhou.ai;

import java.util.HashMap;
import java.util.Map;

public class SseResult {

    private final String type;
    private final Map<String, Object> data = new HashMap<>();

    public SseResult(String type) {
        this.type = type;
    }

    public static SseResult type(String type) {
        return new SseResult(type);
    }

    public SseResult message(String message) {
        this.data.put("message", message);
        return this;
    }

    public SseResult messageId(String messageId) {
        this.data.put("messageId", messageId);
        return this;
    }

    public SseResult content(String content) {
        this.data.put("content", content);
        return this;
    }

    public SseResult code(String code) {
        this.data.put("code", code);
        return this;
    }

    public String type() {
        return this.type;
    }

    public Map<String, Object> data() {
        return this.data;
    }
}
