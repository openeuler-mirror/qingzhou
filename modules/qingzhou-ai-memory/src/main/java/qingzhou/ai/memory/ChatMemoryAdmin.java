package qingzhou.ai.memory;

import java.util.List;

import qingzhou.llm.HistoryMessage;

/**
 * 会话治理接口（管理面业务）：会话列表、全量消息、删除、改名。通用记忆抽象
 * {@link qingzhou.llm.ChatMemory} 只承载对话路径读写，治理属应用业务故定义在本模块，
 * 与对话共用同一实现类（由 {@link ChatMemoryProvider} 同时以两个 OSGi 服务注册）。
 * <p>
 * 删除/改名对不存在、id 非法或归属不符的会话返回 {@code false}（供 HTTP 层区分 404，
 * 不静默成功）；listMessages 在会话不存在或加载失败时返回空列表。
 * userId 一律来自服务端鉴权解析，实现须按用户隔离存储并校验归属，防横向越权。
 */
public interface ChatMemoryAdmin {

    /** 列出用户的会话摘要（不含消息内容），按最近更新时间倒序 */
    List<ConversationSummary> listConversations(String userId);

    /** 取会话全量消息（时间正序），区别于 ChatMemory#recentHistory 的上下文截断 */
    List<HistoryMessage> listMessages(String userId, String conversationId);

    boolean deleteConversation(String userId, String conversationId);

    boolean renameConversation(String userId, String conversationId, String title);
}
