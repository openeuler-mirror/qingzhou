package qingzhou.llm;

import java.util.Collection;

public interface ChatModel {
    void chat(String message, Listener listener, Attachment... attachment);

    interface Builder {
        Builder withDoc(String[] docs);

        Builder withTool(Collection<Tool> tools);

        Builder withSkill(Collection<Skill> skills);

        ChatModel build();
    }
}
