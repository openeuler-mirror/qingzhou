package qingzhou.llm.openai;

import qingzhou.llm.LlmDialect;

public interface OpenAiDialect extends LlmDialect {
    OpenAiDialect reasoningEffort(ReasoningEffort effort);

    OpenAiDialect imageDetail(ImageDetail imageDetail);
}
