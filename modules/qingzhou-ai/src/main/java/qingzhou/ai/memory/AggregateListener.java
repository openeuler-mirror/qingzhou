package qingzhou.ai.memory;

import qingzhou.llm.Listener;

/**
 * 对话落库包装器（记忆启用时由 AiChat 挂载）：跨工具调用轮聚合正文与用量，
 * 正常完成时把 AI 回复写入会话存储并刷新索引排序；出错/中止不落库，即失败轮次不进后续历史。
 * 收尾事件（RUN_FINISHED）先于落库发出，落库失败仅记日志，不影响对话收尾。
 */
public class AggregateListener implements Listener {
    private final Listener delegate;
    private final ConversationStore store;
    private final ConversationIndex index;
    private final String username;
    private final String conversationId;
    private final String messageId;
    private final StringBuilder content = new StringBuilder(); // 思考内容不落库，仅聚合正文
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public AggregateListener(Listener delegate, ConversationStore store, ConversationIndex index,
                             String username, String conversationId, String messageId) {
        this.delegate = delegate;
        this.store = store;
        this.index = index;
        this.username = username;
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    @Override
    public void onReasoning(String content) {
        delegate.onReasoning(content);
    }

    @Override
    public void onReasoningPause() {
        delegate.onReasoningPause();
    }

    @Override
    public void onToolCall(String toolName) {
        delegate.onToolCall(toolName);
    }

    @Override
    public void onSkillMatching() {
        delegate.onSkillMatching();
    }

    @Override
    public void onStillActive() {
        delegate.onStillActive();
    }

    @Override
    public void onMessage(String content) {
        if (content != null) this.content.append(content);
        delegate.onMessage(content);
    }

    @Override
    public void onUsage(int promptTokens, int completionTokens, int totalTokens) {
        // 工具调用多轮时每轮各报一次，落库口径：累加
        this.promptTokens += promptTokens;
        this.completionTokens += completionTokens;
        this.totalTokens += totalTokens;
        delegate.onUsage(promptTokens, completionTokens, totalTokens);
    }

    @Override
    public void onComplete() {
        delegate.onComplete(); // 收尾事件先行
        store.appendAssistant(conversationId, messageId, content.toString(), promptTokens, completionTokens, totalTokens);
        index.touch(username, conversationId);
    }

    @Override
    public void onError(String error) {
        delegate.onError(error);
    }
}
