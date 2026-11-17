package qingzhou.ai.memory.impl;

import java.util.ArrayList;

/**
 * 配额与分层淘汰：超限时按 updatedAt 升序（最久未写入）降级——先删最旧归档，再把最旧活跃转为归档，最后才删除。
 * 调用方须持有 ConversationRepository 锁：配额只做 KV 读写，且必须与触发它的写入原子，否则并发写入会各自判定超限而过度归档
 */
final class ConversationQuota {
    private final ConversationRepository repository;
    private final MemoryLimits limits;

    ConversationQuota(ConversationRepository repository, MemoryLimits limits) {
        this.repository = repository;
        this.limits = limits;
    }

    void enforce(String triggerUser) {
        if (triggerUser != null) {
            enforceUser(triggerUser);
        } else {
            for (String userId : new ArrayList<>(repository.catalog().userIds())) {
                enforceUser(userId);
            }
        }

        ConversationCatalog catalog = repository.catalog();
        // 常态写入在此返回：只比计数，不做全表排序
        if (catalog.withinGlobalQuota(limits)) return;

        // 候选各取一次按 updatedAt 的快照并按序处理：原先每淘汰一个都全表扫描，规模大时退化为 O(n²)
        for (CatalogEntry entry : catalog.sortedByAge(null, false)) {
            if (catalog.activeCount() <= limits.maxConversations) break;
            if (!archiveActive(entry.id)) break;
        }
        // 归档体只剩标题与摘要，价值最低，先出局；活跃体则先归档再考虑删除
        for (CatalogEntry entry : catalog.sortedByAge(null, true)) {
            if (catalog.totalBytes() <= limits.maxBytes) break;
            if (!repository.tryDelete(entry.id)) break; // 落库失败即退出，避免持锁原地打转
        }
        for (CatalogEntry entry : catalog.sortedByAge(null, false)) {
            if (catalog.totalBytes() <= limits.maxBytes) break;
            if (!archiveActive(entry.id)) break;
        }
        for (CatalogEntry entry : catalog.sortedByAge(null, true)) {
            if (catalog.archivedCount() <= limits.maxArchived) break;
            if (!repository.tryDelete(entry.id)) break;
        }
    }

    private void enforceUser(String userId) {
        ConversationCatalog catalog = repository.catalog();
        int limit = limits.activeLimit(userId);
        if (catalog.activeOf(userId) <= limit) return;
        for (CatalogEntry entry : catalog.sortedByAge(userId, false)) {
            if (catalog.activeOf(userId) <= limit) break;
            if (!archiveActive(entry.id)) break;
        }
    }

    // 动手前重读实时状态：快照条目已被前一步归档或删除时跳过，不重复处理
    private boolean archiveActive(String conversationId) {
        CatalogEntry current = repository.catalog().get(conversationId);
        if (current == null || current.archived) return true;
        return archive(current);
    }

    /**
     * 归档：正文清空、标题与摘要保留、updatedAt 不变（归档不该把旧会话顶到列表最前）
     */
    private boolean archive(CatalogEntry entry) {
        ConversationRecord record = repository.load(entry.id);
        if (record == null) {
            repository.catalog().reindex(entry.id, null); // 索引残留：直接摘除
            return true;
        }
        if (record.messages == null || record.messages.isEmpty()) {
            boolean nothingToKeep = (record.summary == null || record.summary.isEmpty())
                    && (record.title == null || record.title.isEmpty());
            if (nothingToKeep) return repository.tryDelete(entry.id); // 标题与摘要皆空，无内容可留
        }
        if (record.summary == null || record.summary.isEmpty()) {
            record.summary = MemoryTexts.spliceSummary(null, record.messages, limits.summaryMaxChars);
        }
        record.messages = null;
        record.archived = true;
        // 落库失败即视为未处理：调用方的淘汰循环据此退出，避免持锁原地打转
        return repository.tryPersist(entry.id, record);
    }
}
