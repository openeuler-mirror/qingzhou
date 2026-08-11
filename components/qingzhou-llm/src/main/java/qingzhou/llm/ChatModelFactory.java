package qingzhou.llm;

import java.util.Collection;
import java.util.List;

public interface ChatModelFactory {
    ChatModelBuilder newChatModelBuilder();

    Attachment newImageAttachment(String base64);

    interface ChatModelBuilder {
        ChatModelBuilder systemPrompt(String systemPrompt);

        ChatModelBuilder docs(List<String> docs);

        ChatModelBuilder tools(Collection<Tool> tools);

        ChatModelBuilder skills(Collection<Skill> skills);

        ChatModel build();
    }
}
