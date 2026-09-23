package qingzhou.ai.memory;

import java.util.List;

final class MemoryTexts {
    static String truncate(String text, int maxChars) {
        if (text == null) return null;
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    /**
     * 拼接式摘要：LLM 不可用时的降级产物，内容截断到摘要上限
     */
    static String spliceSummary(String previousSummary, List<StoredMessage> messages, int maxChars) {
        StringBuilder summary = new StringBuilder();
        if (previousSummary != null && !previousSummary.isEmpty()) {
            summary.append(previousSummary).append('\n');
        }
        if (messages != null) {
            for (StoredMessage message : messages) {
                if (message.content == null || message.content.isEmpty()) continue;
                summary.append("user".equals(message.role) ? "用户：" : "助手：").append(message.content).append('\n');
            }
        }
        return truncate(summary.toString().trim(), maxChars);
    }

    static int length(String text) {
        return text == null ? 0 : text.length();
    }
}
