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
        /**
         * 单篇技能说明书/参考文档注入系统提示的字符上限默认值。
         * 调用方按注入量估算上下文开销时须引用同一常量，避免两处口径脱钩
         */
        int DEFAULT_MAX_DOC_CHARS = 6000;

        ChatModelBuilder systemPrompt(String systemPrompt);

        ChatModelBuilder docs(List<String> docs);

        ChatModelBuilder skills(Collection<Skill> skills);

        ChatModelBuilder tools(Collection<Tool> tools);

        ChatModelBuilder maxToolIterations(int maxToolIterations);

        ChatModelBuilder maxToolResultChars(int maxToolResultChars);

        /**
         * 单篇 技能描述/参考文档，最多注入系统提示的字符数，超出截断
         */
        ChatModelBuilder maxDocChars(int maxDocChars);

        ChatModelBuilder maxRetries(int maxRetries);

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

        LlmDialect getLlmDialect();

        ChatModelBuilder enableThinking(boolean enableThinking);

        ChatModelBuilder chatMemory(ChatMemory chatMemory);

        ChatModelBuilder interceptor(Interceptor interceptor);

        ChatModel build();
    }
}
