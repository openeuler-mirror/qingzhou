package qingzhou.llm.impl.openai;

import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.llm.ChatModel;
import qingzhou.llm.impl.ChatModelBuilderBase;

public class OpenAiChatModelBuilder extends ChatModelBuilderBase {
    private final HttpClient httpClient;
    private final Json json;

    public OpenAiChatModelBuilder(String baseUrl, String apiKey, String model, HttpClient httpClient, Json json) {
        super(baseUrl, apiKey, model);

        this.httpClient = httpClient;
        this.json = json;
    }

    @Override
    protected ChatModel buildInternal() {
        return new OpenAiChatModel(this, httpClient, json);
    }
}
