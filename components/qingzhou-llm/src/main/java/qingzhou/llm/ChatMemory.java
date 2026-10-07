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

    /**
     * 此前对话的摘要：既非用户发言也非助手回答，作为背景事实置于历史之前注入
     */
    interface SummaryMessage extends Message {
        static SummaryMessage of(String content) {
            return () -> content;
        }
    }
}
