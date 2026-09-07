package qingzhou.llm.openai;

import qingzhou.llm.LlmDialect;

import java.util.Map;

public interface OpenAiDialect extends LlmDialect {
    OpenAiDialect reasoningEffort(ReasoningEffort effort);

    OpenAiDialect imageDetail(ImageDetail imageDetail);

    OpenAiDialect responseFormat(ResponseFormat format);

}
