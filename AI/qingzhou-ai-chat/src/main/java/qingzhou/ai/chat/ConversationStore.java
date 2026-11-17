package qingzhou.ai.chat;

import java.util.List;

import qingzhou.llm.ChatMemory;

/**
 * 会话消息存取。消息 id 由调用方预生成：须非空且全局唯一；
 * assistant 消息须与 RUN_STARTED 下发的 id 一致，否则前端问答成对删除会错位
 */
public interface ConversationStore {
    /**
     * @param budgetChars 本轮可注入历史的字符预算（已扣除固定开销与输出预留），不足时按会话侧保底处理
     */
    List<ChatMemory.Message> getMessageList(String userId, String conversationId, int budgetChars);

    void storeUserMessage(String userId, String conversationId, String content, String messageId);

    void storeAssistantMessage(String userId, String conversationId, String content, String messageId);
}
