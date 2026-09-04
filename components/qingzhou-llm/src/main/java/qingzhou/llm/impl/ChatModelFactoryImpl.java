package qingzhou.llm.impl;

import java.net.URL;
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

        // 安全校验：API Key 不应通过明文 HTTP 传输（本机回环除外）
        validateBaseUrl(baseUrl);
    }

    @Override
    public ChatModelBuilder newChatModelBuilder() {
        return new qingzhou.llm.impl.openai.OpenAiChatModelBuilder(baseUrl, apiKey, model, httpClient, json);
    }

    @Override
    public Attachment newImageAttachment(String base64, String mimeType) {
        return new ImageAttachment(base64, mimeType);
    }

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM baseUrl is missing");
        }
        try {
            URL url = new URL(baseUrl);
            String scheme = url.getProtocol();
            String host = url.getHost();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Unsupported LLM baseUrl protocol: " + scheme);
            }
            if ("http".equalsIgnoreCase(scheme) && !isLoopback(host)) {
                LogUtil.println("The LLM's baseUrl uses the HTTP protocol, which means your apiKey will be exposed on the network. We recommend using the HTTPS protocol.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid LLM baseUrl: " + baseUrl, e);
        }
    }

    private boolean isLoopback(String host) {
        if (host == null) return false;
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
