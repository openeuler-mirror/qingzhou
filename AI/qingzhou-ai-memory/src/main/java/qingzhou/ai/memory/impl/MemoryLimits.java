package qingzhou.ai.memory.impl;

import java.util.Map;

import qingzhou.common.Configs;

/**
 * 容量与历史窗口阈值：全部可配置，memory 与 file 两种存储后端共用同一套数值。
 * 上下文总预算与输出预留归对话侧（决定给历史多少），此处只保留窗口保底与条数上限
 */
final class MemoryLimits {
    final long maxBytes;
    final int maxConversations;
    final int maxPerUser;
    final int maxAnonymous;
    final int maxArchived;
    final int messageMaxChars;
    final int summaryMaxChars;
    final int titleMaxChars;
    final int softMessages;
    final int hardMessages;
    final int keepMessages;
    final int compactionInputMaxChars;
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
        titleMaxChars = Math.toIntExact(configs.between("conversation_title_max_chars", 1L, 1000L, 200L));
        softMessages = configs.atLeast("conversation_soft_messages", 1, 80);
        hardMessages = configs.atLeast("conversation_hard_messages", 1, 150);
        // keep 默认取 soft 的一半：两者相等时稳态下压缩队列恒为 1 条消息，每条消息都会触发一次模型调用
        keepMessages = configs.atLeast("conversation_keep_messages", 1, 40);
        compactionInputMaxChars = configs.atLeast("compaction_input_max_chars", 1, 24000);
        historyMinChars = configs.atLeast("history_min_chars", 1, 2000);
        historyMaxMessages = configs.atLeast("history_max_messages", 1, 20);
        if (keepMessages > softMessages || softMessages > hardMessages) {
            throw new IllegalArgumentException("conversation_keep_messages must be <= soft <= hard, got: "
                    + keepMessages + "/" + softMessages + "/" + hardMessages);
        }
        // 与压缩批量的关系：keep 越接近 soft，单次压缩覆盖的消息越少、模型调用越频繁
        if (keepMessages * 2L > softMessages) {
            throw new IllegalArgumentException("conversation_keep_messages must be <= half of "
                    + "conversation_soft_messages, got: " + keepMessages + "/" + softMessages);
        }
    }

    int activeLimit(String userId) {
        return "anonymous".equals(userId) ? maxAnonymous : maxPerUser;
    }
}
