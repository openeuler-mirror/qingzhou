package qingzhou.llm;

public interface LLM {
    Chat buildChatModel(String baseUrl, String apiKey, String model);
}
