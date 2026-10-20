package qingzhou.ai.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话索引：启动时从 KV 重建，此后列表与配额判定全部走内存，不再反序列化会话记录。
 * 索引是纯派生数据，损坏或缺失都能从 KV 重建，因此不持久化
 */
final class ConversationCatalog {
    private final Map<String, CatalogEntry> entries = new HashMap<>();
    private final Map<String, Integer> activeByUser = new HashMap<>();
    private long totalBytes;
    private int activeCount;
    private int archivedCount;
    private long writeSequence;

    void clear() {
        entries.clear();
        activeByUser.clear();
        totalBytes = 0;
        activeCount = 0;
        archivedCount = 0;
        writeSequence = 0;
    }

    /**
     * 索引的唯一写入口：先摘旧（回退计数）再挂新；next 为 null 表示移除
     */
    void reindex(String conversationId, CatalogEntry next) {
        CatalogEntry previous = entries.remove(conversationId);
        if (previous != null) {
            totalBytes -= previous.bytes;
            if (previous.archived) {
                archivedCount--;
            } else {
                activeCount--;
                countUser(previous.userId, -1);
            }
        }
        if (next != null) {
            next.sequence = ++writeSequence;
            entries.put(conversationId, next);
            totalBytes += next.bytes;
            if (next.archived) {
                archivedCount++;
            } else {
                activeCount++;
                countUser(next.userId, 1);
            }
        }
    }

    CatalogEntry get(String conversationId) {
        return entries.get(conversationId);
    }

    List<CatalogEntry> byUser(String userId) {
        List<CatalogEntry> found = new ArrayList<>();
        for (CatalogEntry entry : entries.values()) {
            if (userId != null && userId.equals(entry.userId)) found.add(entry);
        }
        return found;
    }

    /**
     * 按最久未写入升序排列的会话快照；userId 为 null 表示不限用户。调用方按序淘汰，避免逐个反查全表
     */
    List<CatalogEntry> sortedByAge(String userId, boolean archived) {
        List<CatalogEntry> found = new ArrayList<>();
        for (CatalogEntry entry : entries.values()) {
            if (entry.archived != archived) continue;
            if (userId != null && !userId.equals(entry.userId)) continue;
            found.add(entry);
        }
        found.sort(ConversationCatalog::compareByAge);
        return found;
    }

    private static int compareByAge(CatalogEntry a, CatalogEntry b) {
        if (a.updatedAt != b.updatedAt) return Long.compare(a.updatedAt, b.updatedAt);
        return Long.compare(a.sequence, b.sequence);
    }

    Set<String> userIds() {
        return new HashSet<>(activeByUser.keySet());
    }

    int activeOf(String userId) {
        return activeByUser.getOrDefault(userId, 0);
    }

    long totalBytes() {
        return totalBytes;
    }

    int activeCount() {
        return activeCount;
    }

    int archivedCount() {
        return archivedCount;
    }

    boolean overQuota(MemoryLimits limits) {
        if (totalBytes > limits.maxBytes
                || activeCount > limits.maxConversations
                || archivedCount > limits.maxArchived) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : activeByUser.entrySet()) {
            if (entry.getValue() > limits.activeLimit(entry.getKey())) return true;
        }
        return false;
    }

    private void countUser(String userId, int delta) {
        if (userId == null) return;
        int count = activeByUser.getOrDefault(userId, 0) + delta;
        if (count <= 0) {
            activeByUser.remove(userId);
        } else {
            activeByUser.put(userId, count);
        }
    }
}
