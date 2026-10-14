package qingzhou.llm.impl;

import java.net.URL;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
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
        baseUrl = config.get("base_url");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("qingzhou-llm.base_url is missing");
        }
        apiKey = config.get("api_key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("qingzhou-llm.api_key is missing"); // 缺失时 fail-fast，而非请求期才以 "Bearer null" 暴露
        }
        model = config.get("model");
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("qingzhou-llm.model is missing");
        }

        // 安全校验：API Key 不应通过明文 HTTP 传输（本机回环除外）
        validateBaseUrl(baseUrl);
    }

    @Override
    public ChatModelBuilder newChatModelBuilder() {
        return new qingzhou.llm.impl.openai.OpenAiChatModelBuilder(baseUrl, apiKey, model, httpClient, json, crypto);
    }

    @Override
    public Attachment newImageAttachment(String base64, String mimeType) {
        return new ImageAttachment(base64, mimeType);
    }

    private void validateBaseUrl(String baseUrl) {
        URL url;
        try {
            url = new URL(baseUrl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid LLM baseUrl: " + baseUrl, e);
        }

        // 协议校验在解析成功之后单独抛出，保留精确错误信息而不被笼统的包装吞掉
        String scheme = url.getProtocol();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Unsupported LLM baseUrl protocol: " + scheme);
        }
        if ("http".equalsIgnoreCase(scheme) && !isLoopback(url.getHost())) {
            Utils.println("The LLM's baseUrl uses the HTTP protocol, which means your apiKey will be exposed on the network. We recommend using the HTTPS protocol.");
        }
    }

    private boolean isLoopback(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        // URL.getHost 对 IPv6 字面量返回 "[::1]" 带方括号形式
        if ("localhost".equals(normalized) || "::1".equals(normalized) || "[::1]".equals(normalized)) return true;
        // 127.0.0.0/8 整段均为回环，不止 127.0.0.1
        return normalized.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }
}
