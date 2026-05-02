package qingzhou.llm.impl;

import com.agentsflex.core.model.chat.ChatConfig;
import com.agentsflex.core.model.chat.OpenAICompatibleChatModel;
import org.osgi.service.component.annotations.Component;
import qingzhou.llm.ChatModel;
import qingzhou.llm.LLM;

@Component
public class LLMImpl implements LLM {
    @Override
    public ChatModel buildChatModel(String baseUrl, String apiKey, String model) {
        ChatConfig config = new ChatConfig();

        config.setEndpoint(baseUrl);
        config.setApiKey(apiKey);
        config.setModel(model);

        config.setRetryEnabled(false);
        config.setLogEnabled(false);
        config.setObservabilityEnabled(false);
        return new ChatModelImpl(new OpenAICompatibleChatModel<>(config));
    }
}
