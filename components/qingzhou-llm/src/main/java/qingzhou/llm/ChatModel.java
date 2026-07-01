package qingzhou.llm;

import java.util.Collection;
import java.util.List;

public interface ChatModel {
    void chat(String message, Listener listener, Attachment... attachment);

    interface Builder {
        Builder withDoc(List<String> docs);

        Builder withTool(Collection<Tool> tools);

        Builder withSkill(Collection<Skill> skills);

        ChatModel build();
    }
}
