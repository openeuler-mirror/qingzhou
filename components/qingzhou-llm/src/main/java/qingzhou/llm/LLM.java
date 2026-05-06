package qingzhou.llm;

public interface LLM {
    ChatModel buildChatModel(String baseUrl, String apiKey, String model);
}
