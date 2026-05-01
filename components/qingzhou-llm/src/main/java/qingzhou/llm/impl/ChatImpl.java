package qingzhou.llm.impl;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.model.client.StreamContext;
import qingzhou.llm.Chat;
import qingzhou.llm.StreamListener;

public class ChatImpl implements Chat {
    private final ChatModel chatModel;

    public ChatImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chat(String prompt) {
        return chatModel.chat(prompt);
    }

    @Override
    public void chatStream(String prompt, StreamListener listener) {
        chatModel.chatStream(prompt, new StreamResponseListener() {
            @Override
            public void onMessage(StreamContext context, AiMessageResponse response) {
                String delta = response.getMessage().getContent(); // 仅本次增量
                listener.onMessage(delta);
            }

            @Override
            public void onStart(StreamContext context) {
                listener.onStart();
            }

            @Override
            public void onStop(StreamContext context) {
                listener.onStop();
            }

            @Override
            public void onFailure(StreamContext context, Throwable throwable) {
                listener.onFailure(throwable);
            }
        });
    }
}
