package qingzhou.ai.memory.impl;

import java.util.concurrent.atomic.AtomicLong;

import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.store.Store;

/**
 * 会话记录仓储：KV 是唯一正本，{@link ConversationCatalog} 为纯派生索引，可从 KV 重建。
 * 主写入失败上抛，由调用方结束请求；配额与压缩等维护性动作使用 tryXxx，失败只记录并退出本轮
 */
final class ConversationRepository {
    private final Store kvStore;
    private final Json json;
    private final Logger logger;
    private final ConversationCatalog catalog = new ConversationCatalog();
    private final AtomicLong parseFailures = new AtomicLong();
    private final AtomicLong persistFailures = new AtomicLong();

    ConversationRepository(Store kvStore, Json json, Logger logger) {
        this.kvStore = kvStore;
        this.json = json;
        this.logger = logger;
    }

    ConversationCatalog catalog() {
        return catalog;
    }

    long parseFailures() {
        return parseFailures.get();
    }

    long persistFailures() {
        return persistFailures.get();
    }

    synchronized ConversationRecord load(String conversationId) {
        String value = kvStore.get(conversationId);
        if (value == null) return null;
        try {
            return json.fromJson(value, ConversationRecord.class);
        } catch (Exception e) {
            parseFailures.incrementAndGet();
            logger.warn("failed to parse conversation " + conversationId + ": " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 归属校验：跨用户数据隔离属信任边界
     */
    synchronized ConversationRecord ownedRecord(String userId, String conversationId) {
        ConversationRecord record = load(conversationId);
        return record != null && userId.equals(record.userId) ? record : null;
    }

    synchronized void persist(String conversationId, ConversationRecord record) {
        try {
            record.id = conversationId;
            String value = json.toJson(record);
            kvStore.put(conversationId, value);
            catalog.reindex(conversationId, new CatalogEntry(conversationId, record, MemoryTexts.byteSize(value)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to persist conversation " + conversationId, e);
        }
    }

    synchronized boolean tryPersist(String conversationId, ConversationRecord record) {
        try {
            persist(conversationId, record);
            return true;
        } catch (RuntimeException e) {
            persistFailures.incrementAndGet();
            logger.warn("failed to persist conversation " + conversationId + ": " + e.getMessage());
            return false;
        }
    }

    synchronized void delete(String conversationId) {
        try {
            kvStore.delete(conversationId);
        } catch (RuntimeException e) {
            throw new IllegalStateException("failed to delete conversation " + conversationId, e);
        }
        catalog.reindex(conversationId, null);
    }

    synchronized boolean tryDelete(String conversationId) {
        try {
            delete(conversationId);
            return true;
        } catch (RuntimeException e) {
            persistFailures.incrementAndGet();
            logger.warn("failed to delete conversation " + conversationId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 启动重建：索引是派生数据，坏记录跳过并计数，不阻断其余会话
     */
    synchronized void rebuild() {
        catalog.clear();
        for (String conversationId : kvStore.keys()) {
            String value = kvStore.get(conversationId);
            if (value == null) continue;
            try {
                ConversationRecord record = json.fromJson(value, ConversationRecord.class);
                if (record == null) continue;
                catalog.reindex(conversationId, new CatalogEntry(conversationId, record, MemoryTexts.byteSize(value)));
            } catch (Exception e) {
                parseFailures.incrementAndGet();
                logger.warn("failed to parse conversation " + conversationId + ": " + e.getClass().getSimpleName());
            }
        }
    }
}
