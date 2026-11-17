package qingzhou.ai.memory.impl;

import java.util.List;

final class MemoryTexts {
    private MemoryTexts() {
    }

    /**
     * 按码点截断，避免把代理对切成半个字符写入 KV
     */
    static String truncate(String text, int maxChars) {
        if (text == null) return null;
        if (text.codePointCount(0, text.length()) <= maxChars) return text;
        return text.substring(0, text.offsetByCodePoints(0, maxChars));
    }

    /**
     * 拼接式摘要：LLM 不可用时的降级产物。边拼边截，达到上限即停止，避免为超长会话构造整段临时串
     */
    static String spliceSummary(String previousSummary, List<StoredMessage> messages, int maxChars) {
        StringBuilder summary = new StringBuilder();
        if (previousSummary != null && !previousSummary.isEmpty()) {
            summary.append(previousSummary).append('\n');
        }
        if (messages != null) {
            for (StoredMessage message : messages) {
                if (message.content == null || message.content.isEmpty()) continue;
                if (summary.length() >= maxChars) break;
                summary.append(rolePrefix(message.role)).append(message.content).append('\n');
            }
        }
        return truncate(summary.toString().trim(), maxChars);
    }

    static String rolePrefix(String role) {
        return "user".equals(role) ? "用户：" : "助手：";
    }

    static int length(String text) {
        return text == null ? 0 : text.length();
    }

    // 按字符宽度累加 UTF-8 字节数：量体积无须为每次落库再分配一份全量字节数组
    static long byteSize(String value) {
        if (value == null) return 0;
        long bytes = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x80) {
                bytes += 1;
            } else if (c < 0x800) {
                bytes += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < value.length()
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                bytes += 4;
                i++; // 代理对占 4 字节，跳过低位的重复计数
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }
}
