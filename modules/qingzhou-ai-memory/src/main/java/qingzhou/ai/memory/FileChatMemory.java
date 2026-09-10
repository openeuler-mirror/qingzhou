package qingzhou.ai.memory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import qingzhou.json.Json;
import qingzhou.llm.ChatMemory;
import qingzhou.llm.HistoryMessage;
import qingzhou.logger.Logger;

/**
 * 对话记忆的文件实现：每用户一个目录（目录名为 userId 的 SHA-256 摘要），
 * 每会话一个 JSON 文件；由 {@link ChatMemoryProvider} 按 type=file 实例化，重启后记忆仍在。
 * 会话记录内校验 userId 归属，不一致视为不存在；入参 conversationId 先过格式校验防路径遍历。
 */
public class FileChatMemory implements ChatMemory, ChatMemoryAdmin {
    private static final int MAX_MESSAGES_PER_CONVERSATION = 200;
    private static final int MAX_CONVERSATIONS_PER_USER = 50;
    private static final int MAX_HISTORY_CHARS = 8000;

    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private final Json json;
    private final Logger logger;

    private File root;

    public FileChatMemory(Json json, Logger logger) {
        this.json = json;
        this.logger = logger;
        root = new File(System.getProperty("qingzhou.instance"), "data" + File.separator + "ai-conversations");
        if (!root.exists() && !root.mkdirs()) {
            logger.warn("failed to create ai-conversations dir: " + root.getAbsolutePath());
        }
    }

    public static class StoredMessage {
        public String id;
        public String role;
        public String content;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public long createdAt;
    }

    public static class ConversationRecord {
        public String id;
        public String userId;
        /** null 表示未命名 */
        public String title;
        public long createdAt;
        public long updatedAt;
        public List<StoredMessage> messages = new ArrayList<>();
    }

    @Override
    public synchronized String resolveConversationId(String userId, String requestedId) {
        if (requestedId != null) {
            String normalized = requestedId.trim();
            if (VALID_ID.matcher(normalized).matches()) {
                try {
                    ConversationRecord record = load(userId, normalized);
                    if (record == null || userId.equals(record.userId)) {
                        return normalized;
                    }
                } catch (Throwable t) {
                    logger.warn("failed to resolve conversation: " + t.getMessage());
                }
            }
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public synchronized void appendUserMessage(String userId, String conversationId, String content) {
        StoredMessage msg = new StoredMessage();
        msg.id = UUID.randomUUID().toString();
        msg.role = "user";
        msg.content = truncate(content, MAX_HISTORY_CHARS);
        msg.createdAt = System.currentTimeMillis();
        append(userId, conversationId, msg);
    }

    @Override
    public synchronized void appendAssistantMessage(String userId, String conversationId, String messageId, String content,
                                                    int promptTokens, int completionTokens, int totalTokens) {
        StoredMessage msg = new StoredMessage();
        msg.id = messageId;
        msg.role = "assistant";
        msg.content = truncate(content, MAX_HISTORY_CHARS);
        msg.promptTokens = promptTokens;
        msg.completionTokens = completionTokens;
        msg.totalTokens = totalTokens;
        msg.createdAt = System.currentTimeMillis();
        append(userId, conversationId, msg);
    }

    @Override
    public synchronized List<HistoryMessage> recentHistory(String userId, String conversationId, int maxMessages) {
        List<HistoryMessage> history = listMessages(userId, conversationId);
        int from = Math.max(0, history.size() - maxMessages);
        // 返回拷贝而非 subList 视图：方法返回后锁已释放，视图会反映后续 append 的结构性变更
        return new ArrayList<>(history.subList(from, history.size()));
    }

    @Override
    public synchronized List<HistoryMessage> listMessages(String userId, String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        if (!VALID_ID.matcher(conversationId).matches()) return messages;
        try {
            ConversationRecord record = load(userId, conversationId);
            if (record != null) {
                for (StoredMessage msg : record.messages) {
                    if (msg.content == null || msg.content.isEmpty()) continue;
                    messages.add(new HistoryMessage(msg.role, msg.content));
                }
            }
        } catch (Throwable t) {
            logger.warn("failed to load conversation messages: " + t.getMessage());
        }
        return messages;
    }

    @Override
    public synchronized List<ConversationSummary> listConversations(String userId) {
        List<ConversationSummary> summaries = new ArrayList<>();
        File[] files = userDir(userId).listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return summaries;
        for (File file : files) {
            String conversationId = file.getName().substring(0, file.getName().length() - ".json".length());
            try {
                ConversationRecord record = load(userId, conversationId);
                if (record != null) {
                    summaries.add(new ConversationSummary(conversationId, record.title, record.updatedAt));
                }
            } catch (Throwable t) {
                logger.warn("failed to load conversation summary: " + t.getMessage());
            }
        }
        summaries.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return summaries;
    }

    @Override
    public synchronized boolean deleteConversation(String userId, String conversationId) {
        if (!VALID_ID.matcher(conversationId).matches()) return false;
        File file = conversationFile(userId, conversationId);
        return file.exists() && file.delete();
    }

    @Override
    public synchronized boolean renameConversation(String userId, String conversationId, String title) {
        if (!VALID_ID.matcher(conversationId).matches()) return false;
        try {
            ConversationRecord record = load(userId, conversationId);
            if (record == null) return false;
            record.title = title == null ? null : title.trim();
            record.updatedAt = System.currentTimeMillis();
            save(record);
            return true;
        } catch (Throwable t) {
            logger.warn("failed to rename conversation: " + t.getMessage());
            return false;
        }
    }

    private void append(String userId, String conversationId, StoredMessage msg) {
        try {
            ConversationRecord record = load(userId, conversationId);
            if (record == null) {
                record = new ConversationRecord();
                record.id = conversationId;
                record.userId = userId;
                record.createdAt = msg.createdAt;
            }
            record.messages.add(msg);
            while (record.messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                record.messages.remove(0);
            }
            record.updatedAt = msg.createdAt;
            save(record);
            enforceUserQuota(userId);
        } catch (Throwable t) {
            logger.warn("failed to persist conversation message: " + t.getMessage());
        }
    }

    /** 文件缺失、内容损坏或归属用户不符时返回 null */
    private ConversationRecord load(String userId, String conversationId) throws IOException {
        File file = conversationFile(userId, conversationId);
        if (!file.exists()) return null;
        try {
            ConversationRecord record = json.fromJson(
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), ConversationRecord.class);
            if (record == null || record.userId == null || !record.userId.equals(userId)) {
                return null;
            }
            if (record.messages == null) record.messages = new ArrayList<>();
            return record;
        } catch (Throwable t) {
            logger.warn("failed to parse conversation file " + file.getName() + ": " + t.getMessage());
            return null;
        }
    }

    /** Json.toJson 声明抛 Exception，由调用方兜底 catch 处理 */
    private void save(ConversationRecord record) throws Exception {
        File file = conversationFile(record.userId, record.id);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("failed to create dir: " + parent.getAbsolutePath());
        }
        Files.write(file.toPath(), json.toJson(record).getBytes(StandardCharsets.UTF_8));
    }

    private void enforceUserQuota(String userId) {
        File dir = userDir(userId);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length <= MAX_CONVERSATIONS_PER_USER) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - MAX_CONVERSATIONS_PER_USER; i++) {
            if (!files[i].delete()) logger.warn("failed to delete expired conversation: " + files[i].getName());
        }
    }

    private File conversationFile(String userId, String conversationId) {
        return new File(userDir(userId), conversationId + ".json");
    }

    /** 用户目录取 SHA-256 摘要：userId 可能来自 OAuth2，不能直接作为文件名 */
    private File userDir(String userId) {
        return new File(root, sha256Hex(userId));
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest(text.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return null;
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
