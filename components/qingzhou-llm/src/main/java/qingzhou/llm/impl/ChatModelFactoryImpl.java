package qingzhou.llm.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.client.HttpClient;
import qingzhou.json.Json;
import qingzhou.llm.Attachment;
import qingzhou.llm.ChatModelFactory;

@Component(configurationPid = "qingzhou-llm", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChatModelFactoryImpl implements ChatModelFactory {

    @Reference
    private Json json;

    @Reference
    private HttpClient httpClient;

    private String baseUrl;
    private String apiKey;
    private String model;

    @Activate
    public void init(Map<String, String> config) {
        baseUrl = config.get("base_url");
        apiKey = config.get("api_key");
        model = config.get("model");
    }

    @Override
    public ChatModelBuilder newChatModelBuilder() {
        return new qingzhou.llm.impl.openai.OpenAiChatModelBuilder(baseUrl, apiKey, model, json);
    }

    @Override
    public Attachment newImageAttachment(String base64) {
        return new ImageAttachment(base64);
    }
}
