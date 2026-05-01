package qingzhou.llm;

public interface LLM {
    Chat buildChat(String baseUrl, String apiKey, String model);
}
