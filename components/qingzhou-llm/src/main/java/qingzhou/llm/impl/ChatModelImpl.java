package qingzhou.llm.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.message.AssistantMessage;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.tool.FunctionTool;
import org.noear.solon.ai.chat.tool.FunctionToolDesc;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Parameter;
import qingzhou.llm.Tool;

public class ChatModelImpl implements ChatModel {
    private final org.noear.solon.ai.chat.ChatModel chatModel;
    private final String[] systemMessage;

    public ChatModelImpl(org.noear.solon.ai.chat.ChatModel chatModel, String... systemMessage) {
        this.chatModel = chatModel;
        this.systemMessage = systemMessage;
    }

    @Override
    public void chat(String prompt, Collection<Tool> tools, Listener listener) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemMessage != null) {
            for (String s : systemMessage) {
                if (s != null && !s.isEmpty()) {
                    messages.add(ChatMessage.ofSystem(s));
                }
            }
        }
        messages.add(ChatMessage.ofUser(prompt));

        chatModel.prompt(messages)
                .options(op -> op.toolAdd(tools.stream().map(t -> convertTool(t, listener)).collect(Collectors.toSet())))
                .stream()
                .doOnSubscribe(subscription -> listener.onBegin())
                .doOnNext(chatResponse -> doOnNext(chatResponse, listener))
                .doOnComplete(listener::onComplete)
                .doOnError(listener::onError)
                .doOnCancel(listener::onComplete)
                .subscribe();
    }

    private void doOnNext(ChatResponse chatResponse, Listener listener) {
        AssistantMessage aiMessage = chatResponse.getMessage();
        if (aiMessage.isThinking()) {
            String reasoning = aiMessage.getReasoning();
            if (notEmpty(reasoning)) {
                listener.onReasoning(reasoning);
            }
        } else {
            String resultContent = aiMessage.getContent();
            if (notEmpty(resultContent)) {
                listener.onMessage(resultContent);
            }
        }
    }

    private boolean notEmpty(String content) {
        return content != null && !content.isEmpty();
    }

    private FunctionTool convertTool(Tool tool, Listener listener) {
        FunctionToolDesc functionTool = new FunctionToolDesc(tool.name())
                .description(tool.description())
                .doHandle(args -> {
                    listener.onReasoningPause();
                    String result = tool.invoke(args);
                    listener.onToolCall(tool.name(), args, result);
                    listener.onReasoningResume();
                    return result;
                });

        Parameter[] parameters = tool.parameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                functionTool.paramAdd(parameter.name(), String.class, parameter.required(), parameter.description(), null, null);
                String[] list = parameter.enumValues();
                if (list != null && list.length != 0) {
                    String enumValues = Arrays.toString(list);
                    functionTool.description(functionTool.description() + " Optional values: " + enumValues + ", you can only choose one of these values as input.");
                }
            }
        }
        return functionTool;
    }
}
