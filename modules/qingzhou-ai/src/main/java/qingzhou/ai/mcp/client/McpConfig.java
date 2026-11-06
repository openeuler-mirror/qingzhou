package qingzhou.ai.mcp.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import qingzhou.crypto.Crypto;
import qingzhou.json.Json;

final class McpConfig {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    final String id;
    final String url;
    final String description;
    final Map<String, String> headers;

    McpConfig(String id, String url, String description, Map<String, String> headers) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.headers = headers;
    }

    @SuppressWarnings("unchecked")
    static List<McpConfig> load(Path file, Json json, Crypto crypto) throws Exception {
        Map<String, Object> root = json.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), Map.class);
        Map<String, Object> servers = (Map<String, Object>) root.get("mcpServers");

        List<McpConfig> configs = new ArrayList<>(servers.size());
        servers.forEach((id, value) -> {
            Map<String, Object> entry = (Map<String, Object>) value;
            Object enabled = entry.get("enabled"); // 未配置时默认启用
            if (enabled != null && !Boolean.parseBoolean(String.valueOf(enabled))) return;

            Map<String, String> headers = new LinkedHashMap<>();
            Map<String, Object> raw = (Map<String, Object>) entry.getOrDefault("headers", Collections.emptyMap());
            raw.forEach((name, text) -> {
                String headerValue = String.valueOf(text);

                // HTTP header 名/scheme 大小写不敏感；仅 Bearer 凭证是加密项，其余 header 原样透传以免明文被误判为损坏密文
                if (AUTH_HEADER.equalsIgnoreCase(name)
                        && headerValue.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                    headerValue = headerValue.substring(0, BEARER_PREFIX.length()) +
                            crypto.getGlobalCipher().tryDecrypt(
                                    headerValue.substring(BEARER_PREFIX.length()),
                                    "mcpServers." + id + ".headers." + name);
                }

                headers.put(name, headerValue);
            });
            configs.add(new McpConfig(id, (String) entry.get("url"), (String) entry.get("description"), headers));
        });
        return configs;
    }
}
