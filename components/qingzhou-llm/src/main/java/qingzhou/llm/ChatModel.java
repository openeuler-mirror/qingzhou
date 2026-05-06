package qingzhou.llm;

import java.util.Collection;

public interface ChatModel {
    void generate(String prompt, Collection<Tool> tools, Listener listener);
}
