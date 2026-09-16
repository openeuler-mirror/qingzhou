package qingzhou.llm;

import java.util.List;

public interface ChatMemory {
    List<Message> getMessageList();

    interface Message {
        String content();
    }

    interface UserMessage extends Message {
        static UserMessage of(String content) {
            return () -> content;
        }
    }

    interface AssistantMessage extends Message {
        static AssistantMessage of(String content) {
            return () -> content;
        }
    }
}
