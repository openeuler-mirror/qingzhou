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

        ChatModelBuilder reasoningEffort(ReasoningEffort reasoningEffort);

        ChatModelBuilder docs(List<String> docs);

        ChatModelBuilder tools(Collection<Tool> tools);

        ChatModelBuilder dynamicTool(Collection<Tool> tools);

        ChatModelBuilder skills(Collection<Skill> skills);

        ChatModelBuilder imageDetail(ImageDetail imageDetail);

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

        ChatModel build();
    }

    enum ImageDetail {
        low, high, auto
    }
}
