package qingzhou.ai.memory;

import java.util.Map;

/**
 * 容量与预算阈值：全部可配置，memory 与 file 两种存储后端共用同一套数值
 */
final class MemoryLimits {
    final long maxBytes;
    final int maxConversations;
    final int maxPerUser;
    final int maxAnonymous;
    final int maxArchived;
    final int messageMaxChars;
    final int summaryMaxChars;
    final int softMessages;
    final int hardMessages;
    final int keepMessages;
    final int compactionInputMaxChars;
    final int historyTotalBudget;
    final int historyOutputReserve;
    final int historyMinChars;
    final int historyMaxMessages;

    MemoryLimits(Map<String, String> config) {
        // numberPositive 在解析时同步做正数与取值范围校验，激活期 fail-fast：
        // 非法容量若放行到运行期，会表现为静默降级或死循环（如负 hard 值令 remove(0) 持续执行）
        maxBytes = numberPositive(config, "memory_max_bytes", 209715200L);
        maxConversations = numberPositive(config, "memory_max_conversations", 1000);
        maxPerUser = numberPositive(config, "memory_max_conversations_per_user", 50);
        maxAnonymous = numberPositive(config, "memory_max_conversations_anonymous", 20);
        maxArchived = numberPositive(config, "memory_max_archived", 2000);
        messageMaxChars = numberPositive(config, "message_max_chars", 8000);
        summaryMaxChars = numberPositive(config, "summary_max_chars", 2000);
        softMessages = numberPositive(config, "conversation_soft_messages", 80);
        hardMessages = numberPositive(config, "conversation_hard_messages", 150);
        keepMessages = numberPositive(config, "conversation_keep_messages", 80);
        compactionInputMaxChars = numberPositive(config, "compaction_input_max_chars", 24000);
        historyTotalBudget = numberPositive(config, "history_total_budget", 20000);
        historyOutputReserve = numberPositive(config, "history_output_reserve", 4000);
        historyMinChars = numberPositive(config, "history_min_chars", 2000);
        historyMaxMessages = numberPositive(config, "history_max_messages", 20);
        if (keepMessages > softMessages || softMessages > hardMessages) {
            throw new IllegalArgumentException("conversation_keep_messages must be <= soft <= hard, got: "
                    + keepMessages + "/" + softMessages + "/" + hardMessages);
        }
    }

    /**
     * 历史注入预算：总预算减去固定开销与输出预留；开销为负时不反向放大，且不低于下限
     */
    int historyBudget(int overhead) {
        int budget = historyTotalBudget - overhead - historyOutputReserve;
        if (budget > historyTotalBudget) budget = historyTotalBudget;
        return Math.max(budget, historyMinChars);
    }

    int activeLimit(String userId) {
        return "anonymous".equals(userId) ? maxAnonymous : maxPerUser;
    }

    private static long number(Map<String, String> config, String key, long defaultValue) {
        if (config == null) return defaultValue;
        String value = config.get(key);
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("config '" + key + "' must be an number, got: " + value);
        }
    }

    // 解析并校验为正数：键名只出现一次，校验与解析不分离就不会漏配
    private static long numberPositive(Map<String, String> config, String key, long defaultValue) {
        long value = number(config, key, defaultValue);
        if (value <= 0) {
            throw new IllegalArgumentException("config '" + key + "' must be positive, got: " + value);
        }
        return value;
    }

    // int 字段的重载：同时防超范围值在强转时被静默截断
    private static int numberPositive(Map<String, String> config, String key, int defaultValue) {
        long value = number(config, key, defaultValue);
        if (value <= 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "config '" + key + "' must be a positive number, got: " + value);
        }
        return (int) value;
    }
}
