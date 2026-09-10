package qingzhou.llm;

import java.util.List;

/**
 * 对话记忆抽象：实现由独立模块提供（内存/文件/Redis 等），经 ChatModelFactory.ChatModelBuilder#memory
 * 挂载后，ChatModel 自动完成历史注入、用户消息与回复落库。只承载对话路径读写，
 * 会话治理由实现模块另行提供（如 qingzhou-ai-memory 的 ChatMemoryAdmin，与本接口共用实现类）。
 * <p>
 * 实现须知：userId 一律来自服务端鉴权解析，须按用户隔离存储并校验归属，防横向越权；
 * 容量控制、并发安全与故障降级（加载失败不阻断对话）由实现负责；
 * 失败的回复不会调用 {@link #appendAssistantMessage}，出错轮次不进入后续历史。
 */
public interface ChatMemory {

    /**
     * 解析本轮会话标识：requestedId 格式合法且归属该用户则原样采信，否则生成新 id
     * （首轮会话 id 由前端本地生成、后端采信落库）。
     */
    String resolveConversationId(String userId, String requestedId);

    /** 取拼接上下文的最近历史消息（时间正序、不含本轮、最多 maxMessages 条），加载失败返回空列表 */
    List<HistoryMessage> recentHistory(String userId, String conversationId, int maxMessages);

    /** 追加本轮用户消息（会话不存在时由实现创建并记录归属） */
    void appendUserMessage(String userId, String conversationId, String content);

    /**
     * 追加本轮 AI 回复。messageId 由调用方生成：RUN_STARTED 事件已提前携带，
     * 前端与服务端依赖同一 id 对齐（消息反馈、重试等）。
     */
    void appendAssistantMessage(String userId, String conversationId, String messageId, String content,
                                int promptTokens, int completionTokens, int totalTokens);
}
