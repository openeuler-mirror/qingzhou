package qingzhou.llm.impl;

import java.util.Map;

import org.noear.solon.ai.chat.prompt.Prompt;
import qingzhou.llm.ChatContext;

class ChatContextImpl implements ChatContext {
    private final Prompt prompt;

    ChatContextImpl(Prompt prompt) {
        this.prompt = prompt;
    }

    @Override
    public Map<String, Object> attributes() {
        return prompt.attrs();
    }
}
