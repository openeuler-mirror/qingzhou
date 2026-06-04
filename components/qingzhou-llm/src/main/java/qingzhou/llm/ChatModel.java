package qingzhou.llm;

import java.util.Collection;

public interface ChatModel {
    void chat(String message, String[] refDocs, Collection<Tool> tools, Listener listener);
}
