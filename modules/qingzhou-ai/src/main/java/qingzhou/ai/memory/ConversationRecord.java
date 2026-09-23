package qingzhou.ai.memory;

import java.util.List;

/**
 * KV 内会话记录结构，title 为 null 表示未命名。
 * 新增字段均可缺省，旧记录读取时由 JSON 反序列化自动补默认值
 */
final class ConversationRecord {
    public String id;
    public String userId;
    public String title;
    public long createdAt;
    public long updatedAt;
    public List<StoredMessage> messages;
    public String summary;
    public String summaryUpTo;
    public boolean archived;
}
