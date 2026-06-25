package qingzhou.llm;

public interface ChatModel {
    void chat(String message, Listener listener);
}
