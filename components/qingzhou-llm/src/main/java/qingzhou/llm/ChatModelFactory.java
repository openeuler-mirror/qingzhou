package qingzhou.llm;

import java.util.Collection;
import java.util.List;

public interface ChatModelFactory {
    ChatModelBuilder newChatModelBuilder();

    /**
     * 创建携带 mime 类型信息的图片附件，data URI 将使用真实格式。
     * 默认委托给单参版本（image/jpeg），实现类可覆盖以使用真实 mime。
     */
    Attachment newImageAttachment(String base64, String mimeType);

    interface ChatModelBuilder {
        ChatModelBuilder systemPrompt(String systemPrompt);

        ChatModelBuilder docs(List<String> docs);

        /**
         * 多轮对话历史（按时间正序），将拼接在 system 与本次 user 消息之间；
         * 历史条数与单条长度由调用方截断，实现层原样拼接。
         * 与 {@link #memory} 互斥使用：挂载记忆后由 ChatModel 自动加载历史，无需再手动传入。
         */
        ChatModelBuilder history(List<HistoryMessage> history);

        /**
         * 挂载对话记忆（客户端实现传入，如内存/文件/Redis 实现模块）。设置后 ChatModel
         * 在每次对话时自动：加载历史注入上下文 → 落库用户消息 → 正常完成后落库回复（含用量）；
         * 出错/中止不落库回复。conversationId 须由调用方先行 resolve（供 RUN_STARTED 事件下发），
         * messageId 为本轮回复的稳定标识（前端依赖同一 id 对齐）。
         */
        ChatModelBuilder memory(ChatMemory memory, String userId, String conversationId, String messageId);

        /** 记忆模式下注入上下文的最大历史消息条数，默认 20（实现侧另有单条长度与总量配额） */
        ChatModelBuilder maxHistoryMessages(int maxHistoryMessages);

        ChatModelBuilder tools(Collection<Tool> tools);

        ChatModelBuilder skills(Collection<Skill> skills);

        /**
         * 工具执行结果最多回传给模型的字符数，超出截断（OpenAI 官方建议截断工具结果）
         */
        ChatModelBuilder maxToolResultChars(int maxToolResultChars);

        /**
         * 单篇技能描述/参考文档最多注入系统提示的字符数，超出截断以控制输入 token 消耗
         */
        ChatModelBuilder maxPerRefChars(int maxPerRefChars);

        ChatModelBuilder maxRetries(int maxRetries);

        ChatModelBuilder maxToolIterations(int maxToolIterations);

        /**
         * Sets a specified timeout value, in milliseconds,
         * to be used when opening a communications link to the resource referenced by this URLConnection.
         * If the timeout expires before the connection can be established, a java.net.SocketTimeoutException is raised.
         * A timeout of zero is interpreted as an infinite timeout.
         */
        ChatModelBuilder connectTimeout(int connectTimeout);

        /**
         * Sets the read timeout to a specified timeout, in milliseconds.
         * A non-zero value specifies the timeout when reading from Input stream when a connection is established to a resource.
         * If the timeout expires before there is data available for read, a java.net.SocketTimeoutException is raised.
         * A timeout of zero is interpreted as an infinite timeout.
         */
        ChatModelBuilder readTimeout(int readTimeout);

        ChatModelBuilder enableThinking(boolean enableThinking);

        LlmDialect getLlmDialect();

        ChatModel build();
    }
}
