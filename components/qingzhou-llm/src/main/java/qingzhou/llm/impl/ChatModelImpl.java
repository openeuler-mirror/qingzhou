package qingzhou.llm.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.ToolMessage;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.chat.tool.Parameter;
import com.agentsflex.core.model.client.StreamContext;
import com.agentsflex.core.prompt.MemoryPrompt;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Tool;
import qingzhou.llm.ToolParameter;

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

        generate(memoryPrompt, listener);
    }

    private void generate(MemoryPrompt prompt, Listener listener) {
        chatModel.chatStream(prompt, new StreamResponseListener() {
            @Override
            public void onMessage(StreamContext context, AiMessageResponse response) {
                AiMessage message = response.getMessage();

                String reasoningContent = message.getReasoningContent();
                if (reasoningContent != null) {
                    listener.onReasoning(reasoningContent);
                } else {
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        listener.onMessage(content);
                    }
                }

                if (message.isFinalDelta()) {
                    if (message.hasToolCalls()) {
                        prompt.addMessage(message);
                        List<ToolMessage> toolMessages = response.executeToolCallsAndGetToolMessages();
                        prompt.addMessages(toolMessages);

                        generate(prompt, listener);
                    } else {
                        listener.onComplete();
                    }
                }
            }

            @Override
            public void onFailure(StreamContext context, Throwable throwable) {
                listener.onError(throwable);
            }
        });
    }

    private com.agentsflex.core.model.chat.tool.Tool convertTool(Tool tool) {
        com.agentsflex.core.model.chat.tool.Tool.Builder builder = com.agentsflex.core.model.chat.tool.Tool.builder()
                .name(tool.name())
                .description(tool.description())
                .function(tool::invoke);

        ToolParameter[] parameters = tool.parameters();
        if (parameters != null) {
            for (ToolParameter toolParameter : parameters) {
                builder.addParameter(Parameter.builder()
                        .name(toolParameter.name())
                        .description(toolParameter.description())
                        .type(toolParameter.type().value())
                        .required(toolParameter.required())
                        .enums(toolParameter.enumValues())
                        .build());
            }
        }
        return builder.build();
    }
}
