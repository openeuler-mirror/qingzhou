package qingzhou.llm.impl.openai;

import qingzhou.json.Json;
import qingzhou.llm.ChatModel;
import qingzhou.llm.impl.ChatModelBuilderBase;
import qingzhou.llm.impl.ConnectionManager;

public class OpenAiChatModelBuilder extends ChatModelBuilderBase {
    private final Json json;

    public OpenAiChatModelBuilder(String baseUrl, String apiKey, String model, Json json) {
        super(baseUrl, apiKey, model);
        this.json = json;
    }

    @Override
    protected ChatModel buildInternal() {
        return new OpenAiChatModel(this, new ConnectionManager(baseUrl, apiKey), json);
    }
}
