package qingzhou.llm;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model, long timeout, String... systemMessage);

    EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model);
}
