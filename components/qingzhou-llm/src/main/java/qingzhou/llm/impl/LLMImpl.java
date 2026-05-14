package qingzhou.llm.impl;

import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.dialect.ChatDialectManager;
import org.noear.solon.ai.llm.dialect.openai.OpenaiChatDialect;
import org.noear.solon.ai.llm.dialect.openai.OpenaiResponsesDialect;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.llm.Chat;
import qingzhou.llm.LLM;
import qingzhou.llm.impl.slf4j.Slf4jLogBridge;
import qingzhou.logger.Logger;

@Component
public class LLMImpl implements LLM {
    @Reference
    private Logger logger;

    @Activate
    public void init() {
        Slf4jLogBridge.qingzhouLogger = logger;
    }

    @Override
    public Chat buildChatModel(String baseUrl, String apiKey, String model) {
        ChatDialectManager.register(new OpenaiChatDialect());
        ChatDialectManager.register(new OpenaiResponsesDialect());
        return new ChatImpl(ChatModel
                .of(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .build());
    }
}
