package qingzhou.llm;

import java.util.Collection;

public interface Chat {
    void generate(String prompt, Collection<Tool> tools, Listener listener);
}
