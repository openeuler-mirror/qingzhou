package qingzhou.llm;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model, String... systemMessage);

    EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model);
}
