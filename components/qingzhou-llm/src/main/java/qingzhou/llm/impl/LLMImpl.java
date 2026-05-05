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
        config.setEndpoint(getOpenAiChatEndpoint(baseUrl));
        config.setApiKey(apiKey);
        config.setModel(model);

//        config.setRetryEnabled(false);
        config.setLogEnabled(false);
        config.setObservabilityEnabled(false);
        return new ChatModelImpl(new OpenAICompatibleChatModel<>(config));
    }

    private String getOpenAiChatEndpoint(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String openAiChatEndpoint = "/chat/completions";
        return baseUrl.endsWith(openAiChatEndpoint) ? baseUrl : baseUrl + openAiChatEndpoint;
    }
}
