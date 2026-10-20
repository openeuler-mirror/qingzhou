package qingzhou.ai.memory;

import java.util.ArrayList;

/**
 * 配额与分层淘汰：超限时按 updatedAt 升序（最久未写入）降级——先删最旧归档，再把最旧活跃转为归档，最后才删除。
 * 调用方须持有 ConversationStore 锁：配额只做 KV 读写，且必须与触发它的写入原子，否则并发写入会各自判定超限而过度归档
 */
final class ConversationQuota {
    private final ConversationStore store;
    private final MemoryLimits limits;

    ConversationQuota(ConversationStore store, MemoryLimits limits) {
        this.store = store;
        this.limits = limits;
    }

    void enforce(String triggerUser) {
        ConversationCatalog catalog = store.catalog();
        if (triggerUser != null) {
            enforceUser(catalog, triggerUser);
        } else {
            for (String userId : new ArrayList<>(catalog.userIds())) {
                enforceUser(catalog, userId);
            }
        }

        // 候选各取一次按 updatedAt 的快照并按序处理：原先每淘汰一个都全表扫描，规模大时退化为 O(n²)
        for (CatalogEntry entry : catalog.sortedByAge(null, false)) {
            if (catalog.activeCount() <= limits.maxConversations) break;
            archiveActive(catalog, entry.id);
        }
        // 归档体只剩标题与摘要，价值最低，先出局；活跃体则先归档再考虑删除
        for (CatalogEntry entry : catalog.sortedByAge(null, true)) {
            if (catalog.totalBytes() <= limits.maxBytes) break;
            if (!store.deleteRecord(entry.id)) break; // 落库失败即退出，避免持锁原地打转
        }
        for (CatalogEntry entry : catalog.sortedByAge(null, false)) {
            if (catalog.totalBytes() <= limits.maxBytes) break;
            if (!archiveActive(catalog, entry.id)) break;
        }
        for (CatalogEntry entry : catalog.sortedByAge(null, true)) {
            if (catalog.archivedCount() <= limits.maxArchived) break;
            if (!store.deleteRecord(entry.id)) break;
        }
    }

    private void enforceUser(ConversationCatalog catalog, String userId) {
        int limit = limits.activeLimit(userId);
        for (CatalogEntry entry : catalog.sortedByAge(userId, false)) {
            if (catalog.activeOf(userId) <= limit) break;
            archiveActive(catalog, entry.id);
        }
    }

    // 动手前重读实时状态：快照条目已被前一步归档或删除时跳过，不重复处理
    private boolean archiveActive(ConversationCatalog catalog, String conversationId) {
        CatalogEntry current = catalog.get(conversationId);
        if (current == null || current.archived) return true;
        return archive(catalog, current);
    }

    /**
     * 归档：正文清空、标题与摘要保留、updatedAt 不变（归档不该把旧会话顶到列表最前）
     */
    private boolean archive(ConversationCatalog catalog, CatalogEntry entry) {
        if (entry == null) return false;
        ConversationRecord record = store.load(entry.id);
        if (record == null) {
            catalog.reindex(entry.id, null); // 索引残留：直接摘除
            return true;
        }
        if (record.messages == null || record.messages.isEmpty()) {
            boolean nothingToKeep = (record.summary == null || record.summary.isEmpty())
                    && (record.title == null || record.title.isEmpty());
            if (nothingToKeep) return store.deleteRecord(entry.id); // 标题与摘要皆空，无内容可留
        }
        if (record.summary == null || record.summary.isEmpty()) {
            record.summary = MemoryTexts.spliceSummary(null, record.messages, limits.summaryMaxChars);
        }
        record.messages = null;
        record.archived = true;
        // 落库失败即视为未处理：调用方的淘汰循环据此退出，避免持锁原地打转
        return store.save(entry.id, record);
    }
}
