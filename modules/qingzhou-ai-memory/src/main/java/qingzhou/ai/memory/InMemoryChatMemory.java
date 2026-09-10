package qingzhou.ai.memory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import qingzhou.llm.ChatMemory;
import qingzhou.llm.HistoryMessage;

/**
 * 对话记忆的内存实现：按用户隔离，重启即失效，由 {@link ChatMemoryProvider} 按 type=memory 实例化。
 * 仅保留 role/content，消息 id 与 token 用量由持久化实现（FileChatMemory）承载。
 */
public class InMemoryChatMemory implements ChatMemory, ChatMemoryAdmin {
    private static final int MAX_MESSAGES_PER_CONVERSATION = 200;
    private static final int MAX_CONVERSATIONS_PER_USER = 50;
    private static final int MAX_CONTENT_CHARS = 8000;

    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private static final class Conversation {
        String title;
        long updatedAt;
        final List<HistoryMessage> messages = new ArrayList<>();
    }

    /** userId -> (conversationId -> 会话)；LinkedHashMap 保持插入序供配额淘汰，操作在 convs 锁内串行化 */
    private final Map<String, LinkedHashMap<String, Conversation>> store = new ConcurrentHashMap<>();

    @Override
    public String resolveConversationId(String userId, String requestedId) {
        if (requestedId != null) {
            String normalized = requestedId.trim();
            if (VALID_ID.matcher(normalized).matches()) {
                return normalized;
            }
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public List<HistoryMessage> recentHistory(String userId, String conversationId, int maxMessages) {
        List<HistoryMessage> history = listMessages(userId, conversationId);
        return history.size() > maxMessages
                ? new ArrayList<>(history.subList(history.size() - maxMessages, history.size())) : history;
    }

    @Override
    public void appendUserMessage(String userId, String conversationId, String content) {
        append(userId, conversationId, new HistoryMessage("user", truncate(content)));
    }

    @Override
    public void appendAssistantMessage(String userId, String conversationId, String messageId, String content,
                                       int promptTokens, int completionTokens, int totalTokens) {
        append(userId, conversationId, new HistoryMessage("assistant", truncate(content)));
    }

    @Override
    public List<ConversationSummary> listConversations(String userId) {
        List<ConversationSummary> summaries = new ArrayList<>();
        LinkedHashMap<String, Conversation> convs = store.get(userId);
        if (convs == null) return summaries;
        synchronized (convs) {
            for (Map.Entry<String, Conversation> entry : convs.entrySet()) {
                summaries.add(new ConversationSummary(entry.getKey(), entry.getValue().title, entry.getValue().updatedAt));
            }
        }
        summaries.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return summaries;
    }

    @Override
    public List<HistoryMessage> listMessages(String userId, String conversationId) {
        List<HistoryMessage> messages = new ArrayList<>();
        LinkedHashMap<String, Conversation> convs = store.get(userId);
        if (convs == null) return messages;
        synchronized (convs) {
            Conversation conv = convs.get(conversationId);
            if (conv != null) messages.addAll(conv.messages);
        }
        return messages;
    }

    @Override
    public boolean deleteConversation(String userId, String conversationId) {
        LinkedHashMap<String, Conversation> convs = store.get(userId);
        if (convs == null) return false;
        synchronized (convs) {
            return convs.remove(conversationId) != null;
        }
    }

    @Override
    public boolean renameConversation(String userId, String conversationId, String title) {
        LinkedHashMap<String, Conversation> convs = store.get(userId);
        if (convs == null) return false;
        synchronized (convs) {
            Conversation conv = convs.get(conversationId);
            if (conv == null) return false;
            conv.title = title == null ? null : title.trim();
            conv.updatedAt = System.currentTimeMillis();
            return true;
        }
    }

    private void append(String userId, String conversationId, HistoryMessage msg) {
        if (msg.content == null || msg.content.isEmpty()) return;
        LinkedHashMap<String, Conversation> convs = store.computeIfAbsent(userId, k -> new LinkedHashMap<>());
        synchronized (convs) {
            Conversation conv = convs.computeIfAbsent(conversationId, k -> new Conversation());
            conv.messages.add(msg);
            while (conv.messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                conv.messages.remove(0);
            }
            conv.updatedAt = System.currentTimeMillis();
            while (convs.size() > MAX_CONVERSATIONS_PER_USER) {
                Iterator<String> it = convs.keySet().iterator();
                if (!it.hasNext()) break;
                it.next();
                it.remove();
            }
        }
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() <= MAX_CONTENT_CHARS ? text : text.substring(0, MAX_CONTENT_CHARS);
    }
}
