package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.prompt.MemoryPrompt;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Tool;

public class ChatModelImpl implements ChatModel {
    private final com.agentsflex.core.model.chat.ChatModel chatModel;

    public ChatModelImpl(com.agentsflex.core.model.chat.ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void generate(String prompt, Collection<Tool> tools, Listener listener) {
        MemoryPrompt memoryPrompt = new MemoryPrompt();
        memoryPrompt.addMessage(new UserMessage(prompt));
        memoryPrompt.addTools(tools.stream().map(this::convertTool).collect(Collectors.toList()));

        final boolean[] hasToolCalls = {false};
        do {
            chatModel.chatStream(memoryPrompt, new StreamResponseListener() {
                @Override
                public void onStart(StreamContext context) {
                    listener.onBegin();
                }

                @Override
                public void onMessage(StreamContext context, AiMessageResponse response) {
                    AiMessage message = response.getMessage();

                    String reasoningContent = message.getReasoningContent();
                    if (reasoningContent != null) {
                        listener.onReasoning(reasoningContent);
                    }
                    String content = message.getContent();
                    if (content != null) {
                        listener.onMessage(content);
                    }

                    if (message.isFinalDelta()) {
                        memoryPrompt.addMessage(message);

                        if (message.getToolCalls() != null) {
                            List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
                            memoryPrompt.addMessages(toolMessages);

                            hasToolCalls[0] = true;
                        } else if (!message.hasToolCalls()) {
                            listener.onComplete();
                        }
                    }
                }

                @Override
                public void onStop(StreamContext context) {
                    listener.onComplete();
                }

                @Override
                public void onFailure(StreamContext context, Throwable throwable) {
                    listener.onError(throwable);
                }
            });
        } while (hasToolCalls[0]);
    }

    private com.agentsflex.core.model.chat.tool.Tool convertTool(Tool tool) {
        return null;
    }
}
