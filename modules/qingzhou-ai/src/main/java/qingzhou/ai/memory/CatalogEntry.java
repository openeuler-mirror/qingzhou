package qingzhou.ai.memory;

/**
 * 内存索引条目：纯派生数据，仅用于列表与配额判定，不落库
 */
final class CatalogEntry {
    final String id;
    final String userId;
    final String title;
    final long updatedAt;
    final boolean archived;
    final long bytes;
    final int messageCount;
    /**
     * 写入序：由 ConversationCatalog 在 reindex 时赋值。updatedAt 只有毫秒精度，
     * 同一毫秒内写入的多个会话需要它来定序，否则淘汰对象会随哈希遍历顺序变化
     */
    long sequence;

    CatalogEntry(String id, ConversationRecord record, long bytes) {
        this.id = id;
        this.userId = record.userId;
        this.title = record.title;
        this.updatedAt = record.updatedAt;
        this.archived = record.archived;
        this.bytes = bytes;
        this.messageCount = record.messages == null ? 0 : record.messages.size();
    }
}
