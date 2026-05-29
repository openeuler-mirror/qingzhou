package qingzhou.llm;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model, long timeout, long max_tokens, String... systemMessage);

    EmbeddingModel buildEmbeddingModel(String baseUrl, String apiKey, String model);
}
