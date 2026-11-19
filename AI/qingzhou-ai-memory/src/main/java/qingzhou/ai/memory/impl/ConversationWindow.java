package qingzhou.ai.memory.impl;

import java.util.ArrayList;
import java.util.List;

import qingzhou.llm.ChatMemory;
import qingzhou.logger.Logger;

/**
 * 上下文注入窗口：摘要置于历史之前，历史按剩余预算由新到旧截取
 */
final class ConversationWindow {
    private final ConversationRepository repository;
    private final MemoryLimits limits;
    private final Logger logger;

    ConversationWindow(ConversationRepository repository, MemoryLimits limits, Logger logger) {
        this.repository = repository;
        this.limits = limits;
        this.logger = logger;
    }

    List<ChatMemory.Message> build(String userId, String conversationId, int budgetChars) {
        List<ChatMemory.Message> result = new ArrayList<>();
        ConversationRecord record = repository.ownedRecord(userId, conversationId);
        if (record == null) return result;

        if (record.summary != null && !record.summary.isEmpty()) {
            result.add(ChatMemory.SummaryMessage.of(record.summary));
        }
        if (record.archived || record.messages == null) return result;

        List<StoredMessage> messages = record.messages;
        // 摘要同样占用上下文，须从预算中扣除；预算不足时按保底下限取，保证最近 1 轮问答不丢
        int budget = Math.max(limits.historyMinChars, budgetChars - MemoryTexts.length(record.summary));
        int maxMessages = Math.max(2, limits.historyMaxMessages); // 最近 1 轮问答是硬保底，条数护栏不能压过它
        int from = messages.size();
        int chars = 0;
        while (from > 0 && messages.size() - from < maxMessages) {
            int len = MemoryTexts.length(messages.get(from - 1).content);
            boolean mustKeep = messages.size() - from < 2; // 硬保底：至少保留最近 1 轮问答
            if (!mustKeep && chars + len > budget) break;
            chars += len;
            from--;
        }
        for (StoredMessage message : messages.subList(from, messages.size())) {
            if ("user".equals(message.role)) {
                result.add(ChatMemory.UserMessage.of(message.content));
            } else if ("assistant".equals(message.role)) {
                result.add(ChatMemory.AssistantMessage.of(message.content));
            } else {
                logger.warn("Unknown Message Type: " + message.role);
            }
        }
        return result;
    }
}
