package qingzhou.llm.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.ai.chat.content.ContentBlock;
import org.noear.solon.ai.chat.content.ImageBlock;
import org.noear.solon.ai.chat.interceptor.ChatInterceptor;
import org.noear.solon.ai.chat.interceptor.ToolChain;
import org.noear.solon.ai.chat.interceptor.ToolRequest;
import org.noear.solon.ai.chat.message.AssistantMessage;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.tool.ToolResult;
import qingzhou.llm.*;

public class ChatModelImpl implements ChatModel {
    private final org.noear.solon.ai.chat.ChatModel chatModel;

    private final String[] docs;
    private final Collection<Tool> tools;
    private final Collection<Skill> skills;

    public ChatModelImpl(org.noear.solon.ai.chat.ChatModel chatModel,
                         String[] docs, Collection<Tool> tools, Collection<Skill> skills) {
        this.chatModel = chatModel;

        this.docs = docs;
        this.tools = tools;
        this.skills = skills;
    }

    @Override
    public void chat(String message, Listener listener, Attachment... attachment) {
        if (docs != null && docs.length > 0) {
            message = String.format("%s\n\n Now: %s\n\n References: %s",
                    message, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), Arrays.toString(docs));
        }
        List<ContentBlock> blocks = new ArrayList<>();
        if (attachment != null) {
            for (Attachment attach : attachment) {
                if (attach instanceof ImageAttachment) {
                    blocks.add(ImageBlock.ofBase64(((ImageAttachment) attach).base64));
                }
            }
        }
        ChatMessage chatMessage = ChatMessage.ofUser(message, blocks);
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
