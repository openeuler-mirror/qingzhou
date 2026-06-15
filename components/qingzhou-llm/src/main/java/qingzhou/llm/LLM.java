package qingzhou.llm;

import java.util.Collection;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model,
                             long timeout, String systemPrompt, Collection<Skill> systemSkills);

    EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model);

    RerankingModel buildRerankingModel(String baseUrl, String apiKey, String model);
}
