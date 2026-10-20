package qingzhou.llm.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.Configs;
import qingzhou.common.Hosts;
import qingzhou.crypto.Crypto;
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

    @Reference
    private Crypto crypto;

    private String baseUrl;
    private String apiKey;
    private String model;

    @Activate
    public void init(Map<String, String> config) {
        Configs configs = new Configs(config);
        baseUrl = configs.notBlank("base_url");
        apiKey = configs.notBlank("api_key");
        model = configs.notBlank("model");

        // 安全校验：API Key 不应通过明文 HTTP 传输（本机回环除外）
        validateBaseUrl(baseUrl, configs.getOrDefault("allow_insecure_http", false));
    }

    @Override
    public ChatModelBuilder newChatModelBuilder() {
        return new qingzhou.llm.impl.openai.OpenAiChatModelBuilder(baseUrl, apiKey, model, httpClient, json, crypto);
    }

    @Override
    public Attachment newImageAttachment(String base64, String mimeType) {
        return new ImageAttachment(base64, mimeType);
    }

    private static void validateBaseUrl(String baseUrl, boolean allowInsecureHttp) {
        URL url;
        try {
            url = new URL(baseUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid LLM baseUrl: " + baseUrl, e);
        }

        // 协议校验在解析成功之后单独抛出，保留精确错误信息而不被笼统的包装吞掉
        String scheme = url.getProtocol();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Unsupported LLM baseUrl protocol: " + scheme);
        }
        // 明文 HTTP 会让 apiKey 暴露在网络上：非回环端点须显式确认，避免误配置后静默泄露
        if ("http".equalsIgnoreCase(scheme) && !Hosts.isLoopback(url.getHost()) && !allowInsecureHttp) {
            throw new IllegalArgumentException("the LLM baseUrl uses plaintext HTTP, which exposes your apiKey: " + baseUrl
                    + "; use https, or set qingzhou-llm.allow_insecure_http=true to accept the risk");
        }
    }
}
