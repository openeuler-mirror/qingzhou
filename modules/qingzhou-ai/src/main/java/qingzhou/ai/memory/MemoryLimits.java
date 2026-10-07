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
        maxBytes = number(config, "memory_max_bytes", 209715200L);
        maxConversations = (int) number(config, "memory_max_conversations", 1000);
        maxPerUser = (int) number(config, "memory_max_conversations_per_user", 50);
        maxAnonymous = (int) number(config, "memory_max_conversations_anonymous", 20);
        maxArchived = (int) number(config, "memory_max_archived", 2000);
        messageMaxChars = (int) number(config, "message_max_chars", 8000);
        summaryMaxChars = (int) number(config, "summary_max_chars", 2000);
        softMessages = (int) number(config, "conversation_soft_messages", 80);
        hardMessages = (int) number(config, "conversation_hard_messages", 150);
        keepMessages = (int) number(config, "conversation_keep_messages", 80);
        compactionInputMaxChars = (int) number(config, "compaction_input_max_chars", 24000);
        historyTotalBudget = (int) number(config, "history_total_budget", 20000);
        historyOutputReserve = (int) number(config, "history_output_reserve", 4000);
        historyMinChars = (int) number(config, "history_min_chars", 2000);
        historyMaxMessages = (int) number(config, "history_max_messages", 20);
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
            return defaultValue;
        }
    }
}
