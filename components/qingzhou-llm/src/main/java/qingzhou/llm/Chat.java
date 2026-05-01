package qingzhou.llm;

public interface Chat {
    String chat(String prompt);

    void chatStream(String prompt, StreamListener listener);
}
