package qingzhou.llm.impl;

import java.util.Arrays;
import java.util.Collection;
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

    public ChatModelImpl(org.noear.solon.ai.chat.ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void chat(String message, String[] refDocs, Collection<Tool> tools, Listener listener) {
        ChatMessage chatMessage = refDocs != null && refDocs.length > 0
                ? ChatMessage.ofUserAugment(message, Arrays.toString(refDocs))
                : ChatMessage.ofUser(message);
        chatModel.prompt(chatMessage)
                .options(op -> op.toolAdd(tools.stream().map(t -> convertTool(t, listener)).collect(Collectors.toSet())))
                .stream()
                .doOnSubscribe(subscription -> listener.onBegin())
                .doOnNext(chatResponse -> {
                    try {
                        doOnNext(chatResponse, listener);
                    } catch (Exception e) {
                        // 客户端断开连接时，listener 回调中的 httpResponse.send() 会抛出异常，
                        // 让异常继续向上传播，由 Reactor 自动取消上游 LLM 流
                        throw new RuntimeException(e);
                    }
                })
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
