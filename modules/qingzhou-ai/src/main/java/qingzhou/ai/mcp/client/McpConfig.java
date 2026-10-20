package qingzhou.ai.mcp.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qingzhou.crypto.Crypto;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

final class McpConfig {
    private static final String KEY_SERVERS = "mcpServers";
    private static final String TYPE_HTTP = "http";
    private static final String TYPE_STDIO = "stdio";
    private static final String HEADERS_SECRET = "mcp.headers";

    final String id;
    final String url;
    final String description;
    final Map<String, String> headers;

    private McpConfig(String id, String url, String description, Map<String, String> headers) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.headers = headers;
    }

    static List<McpConfig> load(Json json, Crypto crypto, Logger logger) {
        String instance = System.getProperty("qingzhou.instance");
        if (instance == null) return Collections.emptyList();

        Path file = Paths.get(instance, "conf", "mcp.json");
        if (!Files.isRegularFile(file)) return Collections.emptyList();

        Map<String, Object> root = readRoot(file, json, logger);
        if (root == null) return Collections.emptyList();

        Object servers = root.get(KEY_SERVERS);
        if (!(servers instanceof Map)) return Collections.emptyList();

        List<McpConfig> configs = new ArrayList<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) servers).entrySet()) {
            McpConfig config = convert(String.valueOf(entry.getKey()), entry.getValue(), crypto, logger);
            if (config != null) configs.add(config);
        }
        return configs;
    }

    private static Map<String, Object> readRoot(Path file, Json json, Logger logger) {
        try {
            return json.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            logger.warn("failed to load " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static McpConfig convert(String id, Object value, Crypto crypto, Logger logger) {
        if (!(value instanceof Map)) {
            logger.warn("skip mcp server [" + id + "]: entry is not an object");
            return null;
        }
        Map<?, ?> entry = (Map<?, ?>) value;

        String enabled = text(entry.get("enabled"));
        if ("false".equals(enabled)) {
            return null;
        }

        String type = text(entry.get("type"));
        if (type == null) type = entry.get("url") != null ? TYPE_HTTP : TYPE_STDIO; // 兼容官方示例中省略 type 的写法
        if (!TYPE_HTTP.equalsIgnoreCase(type)) {
            logger.warn("skip mcp server [" + id + "]: unsupported type [" + type + "], only http is supported");
            return null;
        }

        String url = text(entry.get("url"));
        if (url == null || url.isEmpty()) {
            logger.warn("skip mcp server [" + id + "]: missing url");
            return null;
        }

        return new McpConfig(id, url, text(entry.get("description")), headers(entry.get("headers"), crypto));
    }

    private static Map<String, String> headers(Object raw, Crypto crypto) {
        if (!(raw instanceof Map)) return Collections.emptyMap();

        Map<String, String> headers = new LinkedHashMap<>();
        ((Map<?, ?>) raw).forEach((name, value) ->
                headers.put(String.valueOf(name), crypto.getGlobalCipher().tryDecrypt(text(value), HEADERS_SECRET)));
        return headers;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
