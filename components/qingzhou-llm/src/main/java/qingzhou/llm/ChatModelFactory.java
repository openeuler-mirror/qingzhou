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

        ChatModelBuilder tools(Collection<Tool> tools);

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

        ChatModel build();
    }

    enum ImageDetail {
        low, high, auto
    }
}
