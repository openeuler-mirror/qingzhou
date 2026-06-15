package qingzhou.llm.impl;

import java.util.Arrays;
import java.util.Collection;

import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.interceptor.ChatInterceptor;
import org.noear.solon.ai.chat.interceptor.ToolChain;
import org.noear.solon.ai.chat.interceptor.ToolRequest;
import org.noear.solon.ai.chat.message.AssistantMessage;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.tool.ToolResult;
import qingzhou.llm.ChatModel;
import qingzhou.llm.Listener;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

class ChatModelImpl implements ChatModel {
    private final org.noear.solon.ai.chat.ChatModel chatModel;

    ChatModelImpl(org.noear.solon.ai.chat.ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void chat(String message, String[] refDocs, Collection<Tool> tools, Collection<Skill> skills, Listener listener) {
        ChatMessage chatMessage = refDocs != null && refDocs.length > 0
                ? ChatMessage.ofUserAugment(message, Arrays.toString(refDocs))
                : ChatMessage.ofUser(message);
        chatModel.prompt(chatMessage)
                .options(op -> {
                    op.toolAdd(Converter.convertTool(tools));
                    op.skillAdd(Converter.convertSkill(skills));
                    op.interceptorAdd(new ChatInterceptor() {
                        @Override
                        public ToolResult interceptTool(ToolRequest req, ToolChain chain) throws Throwable {
                            listener.onReasoningPause();
                            ToolResult toolResult = ChatInterceptor.super.interceptTool(req, chain);
                            listener.onToolCall(chain.getTool().name(), req.getArgs(), toolResult.getContent());
                            listener.onReasoningResume();
                            return toolResult;
                        }
                    });
                })
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
}
