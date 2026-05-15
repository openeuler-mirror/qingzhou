package qingzhou.llm;

import java.util.Collection;

public interface ChatModel {
    void chat(String prompt, Collection<Tool> tools, Listener listener);
}
