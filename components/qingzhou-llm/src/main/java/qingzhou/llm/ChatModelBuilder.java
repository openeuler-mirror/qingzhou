package qingzhou.llm;

import java.util.Collection;

public interface ChatModelBuilder {
    ChatModelBuilder withDoc(String[] docs);

    ChatModelBuilder withTool(Collection<Tool> tools);

    ChatModelBuilder withSkill(Collection<Skill> skills);

    ChatModel build();
}
