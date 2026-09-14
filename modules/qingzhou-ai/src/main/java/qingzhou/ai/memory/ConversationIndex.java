package qingzhou.ai.memory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

import qingzhou.json.Json;
import qingzhou.kv.KeyValueStore;
import qingzhou.logger.Logger;

/**
 * 会话索引（per-user 目录）：KV namespace {@link #NS}，key=sha256(userId)（键不落真实 userId），
 * value=JSON 数组 [{id,title,updatedAt}]。user 配额 {@link #MAX_CONVERSATIONS_PER_USER}：
 * 注册超出按 updatedAt 挤出最旧，被挤出 id 由调用方联动删除会话数据。
 * 所有方法不抛异常（失败记日志返回空值/false）；未配置记忆时本类不注册服务（随 ConversationStore 装配）。
 */
public class ConversationIndex {
    /** 会话索引所在 KV namespace（文件后端目录 data/kv/ai-chat-index/） */
    public static final String NS = "ai-chat-index";
    /** 每用户会话配额：超出按 updatedAt 挤出最旧（与原 FileChatMemory 语义一致） */
    static final int MAX_CONVERSATIONS_PER_USER = 50;

    private final KeyValueStore kv;
    private final Json json;
    private final Logger logger;

    public ConversationIndex(KeyValueStore kv, Json json, Logger logger) {
        this.kv = kv;
        this.json = json;
        this.logger = logger;
    }

    /** 归属判定（resolve 与列表接口共用）：索引故障时返回 false，走「不存在→采信」分支（与原实现一致） */
    public boolean contains(String userId, String conversationId) {
        return find(load(userId), conversationId) != null;
    }

    /**
     * 注册会话条目（新会话，幂等），超出配额挤出 updatedAt 最旧者；返回被挤出会话 id（调用方删除其数据）。
     */
    public List<String> register(String userId, String conversationId) {
        List<String> evicted = new ArrayList<>();
        long now = System.currentTimeMillis();
        update(userId, entries -> {
            if (find(entries, conversationId) != null) return entries;
            Entry entry = new Entry();
            entry.id = conversationId;
            entry.updatedAt = now;
            entries.add(entry);
            entries.sort(Comparator.comparingLong(e -> e.updatedAt)); // 升序：最旧在前
            while (entries.size() > MAX_CONVERSATIONS_PER_USER) {
                evicted.add(entries.remove(0).id);
            }
            return entries;
        });
        return evicted;
    }

    /** 完成回复后刷新排序时间戳（列表按 updatedAt 倒序：活跃会话靠前） */
    public void touch(String userId, String conversationId) {
        long now = System.currentTimeMillis();
        update(userId, entries -> {
            Entry entry = find(entries, conversationId);
            if (entry != null) entry.updatedAt = now;
            return entries;
        });
    }

    /** 重命名（title null 表示未命名）：未命中返回 false（调用方 404） */
    public boolean rename(String userId, String conversationId, String title) {
        boolean[] renamed = {false};
        long now = System.currentTimeMillis();
        update(userId, entries -> {
            Entry entry = find(entries, conversationId);
            if (entry != null) {
                entry.title = title;
                entry.updatedAt = now;
                renamed[0] = true;
            }
            return entries;
        });
        return renamed[0];
    }

    /** 删除索引条目：未命中返回 false（调用方 404，且不删除会话数据） */
    public boolean remove(String userId, String conversationId) {
        boolean[] removed = {false};
        update(userId, entries -> {
            Entry entry = find(entries, conversationId);
            if (entry != null) {
                entries.remove(entry);
                removed[0] = true;
            }
            return entries;
        });
        return removed[0];
    }

    /** 会话列表（updatedAt 倒序）；索引故障返回空列表 */
    public List<ConversationSummary> list(String userId) {
        List<ConversationSummary> summaries = new ArrayList<>();
        try {
            List<Entry> entries = load(userId);
            if (entries != null) {
                entries.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
                for (Entry entry : entries) {
                    summaries.add(new ConversationSummary(entry.id, entry.title, entry.updatedAt));
                }
            }
        } catch (Throwable t) {
            logger.warn("failed to list conversations: " + t.getMessage());
        }
        return summaries;
    }

    /** 索引读改写统一入口：经 kv.update 原子化；解析/序列化失败保持原值（update 语义下返回 null 会误删索引） */
    private void update(String userId, UnaryOperator<List<Entry>> fn) {
        try {
            kv.update(NS, sha256Hex(userId), current -> {
                try {
                    List<Entry> entries = current == null || current.trim().isEmpty()
                            ? new ArrayList<>() : new ArrayList<>(Arrays.asList(json.fromJson(current, Entry[].class)));
                    List<Entry> updated = fn.apply(entries);
                    return json.toJson(updated);
                } catch (Throwable t) {
                    logger.warn("failed to update conversation index: " + t.getMessage());
                    return current;
                }
            });
        } catch (Throwable t) {
            logger.warn("failed to update conversation index: " + t.getMessage());
        }
    }

    private List<Entry> load(String userId) {
        String value = kv.get(NS, sha256Hex(userId));
        if (value == null) return null;
        try {
            return new ArrayList<>(Arrays.asList(json.fromJson(value, Entry[].class)));
        } catch (Throwable t) {
            logger.warn("failed to load conversation index: " + t.getMessage());
            return null;
        }
    }

    private Entry find(List<Entry> entries, String conversationId) {
        if (entries == null) return null;
        for (Entry entry : entries) {
            if (conversationId.equals(entry.id)) return entry;
        }
        return null;
    }

    /** userId 自由文本不直接进 key：SHA-256 摘要（沿用原 FileChatMemory 约定） */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest(input.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 索引条目（KV 值元素）：title null 表示未命名（未 rename 过） */
    public static class Entry {
        public String id;
        public String title;
        public long updatedAt;
    }
}
