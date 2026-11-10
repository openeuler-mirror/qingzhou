package qingzhou.ai.memory;

import java.util.List;

import qingzhou.llm.ChatMemory;

public interface ConversationStore {
    List<ChatMemory.Message> getMessageList(String userId, String conversationId, int overhead);

    String storeUserMessage(String conversationId, String userId, String content);

    void storeAssistantMessage(String conversationId, String userId, String content, String messageId);
}
