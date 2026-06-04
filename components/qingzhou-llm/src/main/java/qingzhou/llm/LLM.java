package qingzhou.llm;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model, long timeout, String systemPrompt);

    EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model);

    RerankingModel buildRerankingModel(String baseUrl, String apiKey, String model);
}
