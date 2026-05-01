package qingzhou.llm.impl;

import com.agentsflex.core.model.chat.ChatConfig;
import com.agentsflex.core.model.chat.OpenAICompatibleChatModel;
import org.osgi.service.component.annotations.Component;
import qingzhou.llm.Chat;
import qingzhou.llm.LLM;

@Component
public class LLMImpl implements LLM {
    @Override
    public Chat buildChat(String baseUrl, String apiKey, String model) {
        ChatConfig config = new ChatConfig();

        config.setEndpoint(baseUrl);
        config.setApiKey(apiKey);
        config.setModel(model);

        config.setRetryEnabled(false);
        config.setLogEnabled(false);
        config.setObservabilityEnabled(false);
        return new ChatImpl(new OpenAICompatibleChatModel<>(config));
    }
}
