package qingzhou.ai.mcp.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import qingzhou.crypto.Crypto;
import qingzhou.json.Json;

final class McpConfig {
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
            raw.forEach((name, text) ->
                    headers.put(name, crypto.getGlobalCipher().tryDecrypt(String.valueOf(text), "mcpServers." + id + ".headers" + "." + name)));
            configs.add(new McpConfig(id, (String) entry.get("url"), (String) entry.get("description"), headers));
        });
        return configs;
    }
}
