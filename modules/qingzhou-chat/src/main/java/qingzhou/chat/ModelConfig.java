package qingzhou.chat;

import com.agentsflex.core.model.chat.ChatConfig;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.OpenAICompatibleChatModel;

import java.util.Map;

public class ModelConfig {

    public static synchronized ChatModel getChatModel(Map<String, String> properties) {
        if (properties != null && !properties.isEmpty()) {
            ChatConfig config = new ChatConfig();
            config.setProvider(properties.get("provider"));
            config.setEndpoint(properties.get("endpoint"));
            config.setRequestPath(properties.get("requestPath"));
            config.setModel(properties.get("model"));
            config.setApiKey(properties.get("apiKey"));
            config.setLogEnabled(false);
            config.setObservabilityEnabled(false);
            return new OpenAICompatibleChatModel<>(config);
        }

        return null;
    }
}
