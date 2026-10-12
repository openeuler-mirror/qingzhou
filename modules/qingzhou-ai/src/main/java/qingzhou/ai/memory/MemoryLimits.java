package qingzhou.ai.memory;

import java.util.Map;

import qingzhou.common.Configs;

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
        Configs configs = new Configs(config);
        // Configs 在解析时同步做下界与取值范围校验，激活期 fail-fast：
        // 非法容量若放行到运行期，会表现为静默降级或死循环（如负 hard 值令 remove(0) 持续执行）
        maxBytes = configs.atLeast("memory_max_bytes", 1, 209715200L);
        maxConversations = configs.atLeast("memory_max_conversations", 1, 1000);
        maxPerUser = configs.atLeast("memory_max_conversations_per_user", 1, 50);
        maxAnonymous = configs.atLeast("memory_max_conversations_anonymous", 1, 20);
        maxArchived = configs.atLeast("memory_max_archived", 1, 2000);
        messageMaxChars = configs.atLeast("message_max_chars", 1, 8000);
        summaryMaxChars = configs.atLeast("summary_max_chars", 1, 2000);
        softMessages = configs.atLeast("conversation_soft_messages", 1, 80);
        hardMessages = configs.atLeast("conversation_hard_messages", 1, 150);
        keepMessages = configs.atLeast("conversation_keep_messages", 1, 80);
        compactionInputMaxChars = configs.atLeast("compaction_input_max_chars", 1, 24000);
        historyTotalBudget = configs.atLeast("history_total_budget", 1, 20000);
        historyOutputReserve = configs.atLeast("history_output_reserve", 0, 4000); // 0 表示不预留输出预算
        historyMinChars = configs.atLeast("history_min_chars", 1, 2000);
        historyMaxMessages = configs.atLeast("history_max_messages", 1, 20);
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
}
